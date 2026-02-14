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
package org.groundplatform.android.ui.datacollection.tasks.polygon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.databinding.FragmentDrawAreaTaskBinding
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.TaskView
import org.groundplatform.android.ui.datacollection.components.TaskViewFactory
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment.Companion.TASK_ID_FRAGMENT_ARG_KEY
import org.groundplatform.android.ui.datacollection.tasks.launchWhenTaskVisible
import org.groundplatform.android.util.renderComposableDialog

@AndroidEntryPoint
class DrawAreaTaskFragment @Inject constructor() : AbstractTaskFragment<DrawAreaTaskViewModel>() {
  @Inject lateinit var drawAreaTaskMapFragmentProvider: Provider<DrawAreaTaskMapFragment>
  private lateinit var drawAreaTaskMapFragment: DrawAreaTaskMapFragment

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithCombinedHeader(inflater, R.drawable.outline_draw)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    // XML layout is used to provide a static view ID which does not collide with Google Maps view
    // ID (https://github.com/google/ground-android/issues/2493).
    // The ID is needed when restoring the view on config change since the view is dynamically
    // created.
    // TODO: Remove this workaround once this UI is migrated to Compose.
    // Issue URL: https://github.com/google/ground-android/issues/1795
    val rootView = FragmentDrawAreaTaskBinding.inflate(inflater)

    drawAreaTaskMapFragment = drawAreaTaskMapFragmentProvider.get()
    val args = Bundle()
    args.putString(TASK_ID_FRAGMENT_ARG_KEY, taskId)
    drawAreaTaskMapFragment.arguments = args
    childFragmentManager
      .beginTransaction()
      .add(
        R.id.container_draw_area_task_map,
        drawAreaTaskMapFragment,
        DrawAreaTaskMapFragment::class.java.simpleName,
      )
      .commit()

    setupObservables()

    return rootView.root
  }

  override fun onTaskViewAttached() {
    // Collect camera movement events from ViewModel (e.g., after undo/redo)
    viewModel.cameraMoveEvents
      .onEach { coordinates -> drawAreaTaskMapFragment.moveToPosition(coordinates) }
      .launchIn(viewLifecycleOwner.lifecycleScope)
  }

  private fun setupObservables() {
    launchWhenTaskVisible(dataCollectionViewModel, taskId) {
      launch {
        if (!viewModel.instructionsDialogShown) {
          showInstructionsDialog()
        }
      }
      launch {
        viewModel.polygonArea.asFlow().collect { area ->
          Toast.makeText(
              requireContext(),
              getString(R.string.area_message, area),
              Toast.LENGTH_LONG,
            )
            .show()
        }
      }
      launch {
        viewModel.showSelfIntersectionDialog.collect {
          renderComposableDialog {
            ConfirmationDialog(
              title = R.string.polygon_vertex_add_dialog_title,
              description = R.string.polygon_vertex_add_dialog_message,
              confirmButtonText = R.string.polygon_vertex_add_dialog_positive_button,
              dismissButtonText = null,
              onConfirmClicked = {},
            )
          }
        }
      }
    }
  }

  private fun showInstructionsDialog() {
    viewModel.instructionsDialogShown = true
    renderComposableDialog {
      InstructionsDialog(
        iconId = R.drawable.touch_app_24,
        stringId = R.string.draw_area_task_instruction,
      )
    }
  }
}
