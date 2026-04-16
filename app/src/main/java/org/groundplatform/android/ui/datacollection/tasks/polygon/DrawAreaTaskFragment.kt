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

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskMapFragmentContainer
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment

@AndroidEntryPoint
class DrawAreaTaskFragment @Inject constructor() : AbstractTaskFragment<DrawAreaTaskViewModel>() {
  @Inject lateinit var drawAreaTaskMapFragmentProvider: Provider<DrawAreaTaskMapFragment>

  override val taskHeader: TaskHeader by lazy {
    TaskHeader(viewModel.task.label, R.drawable.outline_draw)
  }

  override val instructionData =
    InstructionData(
      iconId = R.drawable.touch_app_24,
      stringId = R.string.draw_area_task_instruction,
    )

  @Composable
  override fun TaskBody() {
    var showSelfIntersectionDialog by viewModel.showSelfIntersectionDialog

    TaskMapFragmentContainer(
      taskId = viewModel.task.id,
      fragmentManager = childFragmentManager,
      fragmentProvider = drawAreaTaskMapFragmentProvider,
    )

    if (showSelfIntersectionDialog) {
      ConfirmationDialog(
        title = R.string.polygon_vertex_add_dialog_title,
        description = R.string.polygon_vertex_add_dialog_message,
        confirmButtonText = R.string.polygon_vertex_add_dialog_positive_button,
        dismissButtonText = null,
        onConfirmClicked = { showSelfIntersectionDialog = false },
      )
    }
  }

  override fun onTaskResume() {
    if (isVisible && !viewModel.instructionsDialogShown) {
      viewModel.showInstructions()
    }
    viewModel.polygonArea.observe(viewLifecycleOwner) { area ->
      Toast.makeText(requireContext(), getString(R.string.area_message, area), Toast.LENGTH_LONG)
        .show()
    }
  }

  override fun onInstructionDialogDismissed() {
    viewModel.instructionsDialogShown = true
  }
}
