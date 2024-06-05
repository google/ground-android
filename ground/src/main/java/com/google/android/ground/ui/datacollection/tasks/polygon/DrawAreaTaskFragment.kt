/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.ground.ui.datacollection.tasks.polygon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LineString.Companion.lineStringOf
import com.google.android.ground.ui.IconFactory
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.InstructionsDialog
import com.google.android.ground.ui.datacollection.components.TaskButton
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DrawAreaTaskFragment : AbstractTaskFragment<DrawAreaTaskViewModel>() {

  @Inject lateinit var markerIconFactory: IconFactory
  @Inject lateinit var map: MapFragment

  // Action buttons
  private lateinit var completeButton: TaskButton
  private lateinit var addPointButton: TaskButton
  private lateinit var nextButton: TaskButton

  private lateinit var drawAreaTaskMapFragment: DrawAreaTaskMapFragment

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithCombinedHeader(inflater, R.drawable.outline_draw)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() }
    drawAreaTaskMapFragment = DrawAreaTaskMapFragment.newInstance(viewModel, map)
    parentFragmentManager
      .beginTransaction()
      .add(rowLayout.id, drawAreaTaskMapFragment, DrawAreaTaskMapFragment::class.java.simpleName)
      .commit()
    return rowLayout
  }

  override fun onCreateActionButtons() {
    addSkipButton()
    addUndoButton { removeLastVertex() }
    nextButton = addNextButton()
    addPointButton =
      addButton(ButtonAction.ADD_POINT).setOnClickListener { viewModel.addLastVertex() }
    completeButton =
      addButton(ButtonAction.COMPLETE).setOnClickListener {
        viewModel.onCompletePolygonButtonClick()
      }
  }

  /** Removes the last vertex from the polygon. */
  private fun removeLastVertex() {
    viewModel.removeLastVertex()

    // Move the camera to the last vertex, if any.
    val lastVertex = viewModel.getLastVertex() ?: return
    drawAreaTaskMapFragment.moveToPosition(lastVertex)
  }

  override fun onTaskViewAttached() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.draftArea.collectLatest { onFeatureUpdated(it) }
    }
  }

  override fun onTaskResume() {
    if (isVisible && !viewModel.instructionsDialogShown) {
      showInstructionsDialog()
    }
  }

  private fun onFeatureUpdated(feature: Feature?) {
    val geometry = feature?.geometry ?: lineStringOf()
    check(geometry is LineString) { "Invalid area geometry type ${geometry.javaClass}" }

    addPointButton.showIfTrue(!geometry.isClosed())
    completeButton.showIfTrue(geometry.isClosed() && !viewModel.isMarkedComplete())
    nextButton.showIfTrue(viewModel.isMarkedComplete())
  }

  private fun showInstructionsDialog() {
    viewModel.instructionsDialogShown = true
    (view as ViewGroup).addView(
      ComposeView(requireContext()).apply {
        setContent {
          val openAlertDialog = remember { mutableStateOf(true) }
          when {
            openAlertDialog.value -> {
              AppTheme {
                InstructionsDialog(R.drawable.touch_app_24, R.string.draw_area_task_instruction) {
                  openAlertDialog.value = false
                }
              }
            }
          }
        }
      }
    )
  }
}
