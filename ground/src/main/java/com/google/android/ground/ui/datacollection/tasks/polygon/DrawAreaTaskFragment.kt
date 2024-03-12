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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LineString.Companion.lineStringOf
import com.google.android.ground.ui.IconFactory
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskButton
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapFragment
import com.google.android.material.color.MaterialColors
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
    addUndoButton()
    nextButton = addNextButton()
    addPointButton =
      addButton(ButtonAction.ADD_POINT).setOnClickListener { viewModel.addLastVertex() }
    completeButton =
      addButton(ButtonAction.COMPLETE).setOnClickListener {
        viewModel.onCompletePolygonButtonClick()
      }
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
              CreateInstructionsDialog { openAlertDialog.value = false }
            }
          }
        }
      }
    )
  }

  @Composable
  private fun CreateInstructionsDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
      icon = {
        Icon(
          imageVector = ImageVector.vectorResource(id = R.drawable.touch_app_24),
          contentDescription = "",
          modifier = Modifier.width(48.dp).height(48.dp),
        )
      },
      title = { Text(text = getString(R.string.draw_area_task_instruction), fontSize = 18.sp) },
      onDismissRequest = {}, // Prevent dismissing the dialog by clicking outside
      confirmButton = {}, // Hide confirm button
      dismissButton = {
        OutlinedButton(onClick = { onDismissRequest() }) {
          Text(
            text = getString(R.string.close),
            color = getMaterialColor(R.attr.colorPrimary),
          )
        }
      },
      containerColor = getMaterialColor(R.attr.colorBackgroundFloating),
      textContentColor = getMaterialColor(R.attr.colorOnBackground),
    )
  }

  private fun getMaterialColor(id: Int): Color =
    Color(MaterialColors.getColor(requireContext(), id, ""))
}
