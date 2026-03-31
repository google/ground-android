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
package org.groundplatform.android.ui.datacollection.tasks.date

import android.text.format.DateFormat
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.submission.DateTimeTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.ui.theme.sizes

@Composable
fun DateTaskScreen(
  viewModel: DateTaskViewModel,
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val taskData by viewModel.taskTaskData.collectAsStateWithLifecycle()

  TaskScreen(
    taskHeader =
      TaskHeader(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer),
    taskActionButtonsStates = taskActionButtonsStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    taskBody = {
      DateTaskContent(
        taskData,
        onDateSelected = { viewModel.updateResponse(it) },
        onResponseCleared = { viewModel.clearResponse() },
      )
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTaskContent(
  taskData: TaskData?,
  onDateSelected: (Long) -> Unit,
  onResponseCleared: () -> Unit,
) {
  val context = LocalContext.current
  var showDialog by rememberSaveable { mutableStateOf(false) }

  val dateText =
    remember(taskData) {
      (taskData as? DateTimeTaskData)?.let {
        DateFormat.getDateFormat(context).format(Date(it.timeInMillis))
      } ?: ""
    }

  val hintText = remember {
    (DateFormat.getDateFormat(context) as SimpleDateFormat).toPattern().uppercase()
  }

  DateInputField(
    modifier = Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding),
    dateText = dateText,
    hintText = hintText,
    onDateClick = { showDialog = true },
  )

  if (showDialog) {
    DateSelectionDialog(
      initialDate = (taskData as? DateTimeTaskData)?.timeInMillis,
      onDateSelected = onDateSelected,
      onClear = {
        onResponseCleared()
        showDialog = false
      },
      onDismiss = { showDialog = false },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectionDialog(
  initialDate: Long?,
  onDateSelected: (Long) -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
) {
  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          datePickerState.selectedDateMillis?.let { onDateSelected(it) }
          onDismiss()
        }
      ) {
        Text(stringResource(android.R.string.ok))
      }
    },
    dismissButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) } },
  ) {
    DatePicker(state = datePickerState)
  }
}
