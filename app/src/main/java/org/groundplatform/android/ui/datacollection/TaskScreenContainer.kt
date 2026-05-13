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
package org.groundplatform.android.ui.datacollection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.ui.datacollection.components.ButtonAction
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
 * @param taskPosition the position of this task within the data collection flow.
 * @param dataCollectionViewModel the [DataCollectionViewModel] for the data collection flow.
 */
@Composable
fun TaskScreenContainer(
  task: Task,
  taskPosition: TaskPosition,
  dataCollectionViewModel: DataCollectionViewModel,
) {
  val taskViewModel = dataCollectionViewModel.getTaskViewModel(task.id) ?: return

  val loiName by dataCollectionViewModel.loiNameDraft.collectAsStateWithLifecycle()
  val showLoiNameDialog by dataCollectionViewModel.loiNameDialogOpen.collectAsStateWithLifecycle()

  val onButtonClicked = { action: ButtonAction -> taskViewModel.onButtonClick(action) }

  val onLoiNameAction = { action: LoiNameAction ->
    dataCollectionViewModel.handleLoiNameAction(action, task.id)
  }

  when (taskViewModel) {
    is CaptureLocationTaskViewModel ->
      CaptureLocationTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
      )

    is DateTaskViewModel ->
      DateTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
      )

    is DrawAreaTaskViewModel ->
      DrawAreaTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
        onLoiNameAction = onLoiNameAction,
        loiName = loiName,
        shouldShowLoiNameDialog = showLoiNameDialog,
      )

    is DropPinTaskViewModel ->
      DropPinTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
        onLoiNameAction = onLoiNameAction,
        loiName = loiName,
        shouldShowLoiNameDialog = showLoiNameDialog,
      )

    is InstructionTaskViewModel ->
      InstructionTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
      )

    is MultipleChoiceTaskViewModel ->
      MultipleChoiceTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
      )

    is PhotoTaskViewModel ->
      PhotoTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
      )

    is NumberTaskViewModel ->
      NumberTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
      )

    is TextTaskViewModel ->
      TextTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
      )

    is TimeTaskViewModel ->
      TimeTaskScreen(
        viewModel = taskViewModel,
        taskPosition = taskPosition,
        onButtonClicked = onButtonClicked,
      )

    else -> error("Unsupported task type: ${task.type}")
  }
}
