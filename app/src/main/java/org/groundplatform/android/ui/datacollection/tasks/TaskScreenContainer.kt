/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks

import androidx.compose.runtime.Composable
import org.groundplatform.android.ui.datacollection.LoiNameAction
import org.groundplatform.android.ui.datacollection.TaskPosition
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskViewModel
import org.groundplatform.domain.model.task.Task

/**
 * A container that renders the appropriate task screen based on the provided [task] type.
 *
 * @param task the task to be displayed.
 * @param taskViewModel the [AbstractTaskViewModel] for the task.
 * @param taskPosition the position of this task within the data collection flow.
 * @param loiName the draft LOI name.
 * @param shouldShowLoiNameDialog whether the LOI name dialog should be shown.
 * @param onLoiNameAction callback for LOI name actions.
 */
@Composable
fun TaskScreenContainer(
  task: Task,
  taskViewModel: AbstractTaskViewModel,
  taskPosition: TaskPosition,
  loiName: String = "",
  shouldShowLoiNameDialog: Boolean = false,
  onLoiNameAction: (LoiNameAction) -> Unit = {},
) {
  when (task.type) {
    Task.Type.CAPTURE_LOCATION ->
      CaptureLocationTaskScreen(
        viewModel = taskViewModel as CaptureLocationTaskViewModel,
        taskPosition = taskPosition,
      )

    Task.Type.DATE ->
      DateTaskScreen(viewModel = taskViewModel as DateTaskViewModel, taskPosition = taskPosition)

    Task.Type.DRAW_AREA ->
      DrawAreaTaskScreen(
        viewModel = taskViewModel as DrawAreaTaskViewModel,
        taskPosition = taskPosition,
        onLoiNameAction = onLoiNameAction,
        loiName = loiName,
        shouldShowLoiNameDialog = shouldShowLoiNameDialog,
      )

    Task.Type.DROP_PIN ->
      DropPinTaskScreen(
        viewModel = taskViewModel as DropPinTaskViewModel,
        taskPosition = taskPosition,
        onLoiNameAction = onLoiNameAction,
        loiName = loiName,
        shouldShowLoiNameDialog = shouldShowLoiNameDialog,
      )

    Task.Type.INSTRUCTIONS ->
      InstructionTaskScreen(
        viewModel = taskViewModel as InstructionTaskViewModel,
        taskPosition = taskPosition,
      )

    Task.Type.MULTIPLE_CHOICE ->
      MultipleChoiceTaskScreen(
        viewModel = taskViewModel as MultipleChoiceTaskViewModel,
        taskPosition = taskPosition,
      )

    Task.Type.PHOTO ->
      PhotoTaskScreen(viewModel = taskViewModel as PhotoTaskViewModel, taskPosition = taskPosition)

    Task.Type.NUMBER ->
      NumberTaskScreen(
        viewModel = taskViewModel as NumberTaskViewModel,
        taskPosition = taskPosition,
      )

    Task.Type.TEXT ->
      TextTaskScreen(viewModel = taskViewModel as TextTaskViewModel, taskPosition = taskPosition)

    Task.Type.TIME ->
      TimeTaskScreen(viewModel = taskViewModel as TimeTaskViewModel, taskPosition = taskPosition)

    else -> error("Unsupported task type: ${task.type}")
  }
}
