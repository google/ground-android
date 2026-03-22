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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.FragmentContainer
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskContainer
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment

@Composable
fun DrawAreaTaskScreen(viewModel: DrawAreaTaskViewModel, env: TaskScreenEnvironment) {
  val context = LocalContext.current
  var drawAreaTaskMapFragment by remember { mutableStateOf<DrawAreaTaskMapFragment?>(null) }
  var showSelfIntersectionDialog by viewModel.showSelfIntersectionDialog

  val taskHeader = TaskHeader(viewModel.task.label, R.drawable.outline_draw)
  val instructionData =
    InstructionData(
      iconId = R.drawable.touch_app_24,
      stringId = R.string.draw_area_task_instruction,
    )

  LaunchedEffect(drawAreaTaskMapFragment) {
    drawAreaTaskMapFragment?.let { fragment ->
      viewModel.cameraMoveEvents.collect { coordinates -> fragment.moveToPosition(coordinates) }
    }
  }

  val polygonArea by viewModel.polygonArea.observeAsState()
  polygonArea?.let { area ->
    val message = stringResource(R.string.area_message, area)
    LaunchedEffect(area) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
  }

  TaskContainer(
    viewModel = viewModel,
    dataCollectionViewModel = env.dataCollectionViewModel,
    taskHeader = taskHeader,
    instructionData = instructionData,
    showInstructionDialog = !viewModel.instructionsDialogShown,
  ) {
    FragmentContainer(
      env = env,
      taskId = viewModel.task.id,
      fragmentProvider = env.drawAreaTaskMapFragmentProvider,
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
}
