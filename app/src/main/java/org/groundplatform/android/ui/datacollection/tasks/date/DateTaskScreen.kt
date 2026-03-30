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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
  onDateSelected: (Date) -> Unit,
  onResponseCleared: () -> Unit,
) {
  val context = LocalContext.current
  var showSheet by remember { mutableStateOf(false) }

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
    onDateClick = { showSheet = true },
  )

  if (showSheet) {
    DateSelectionBottomSheet(
      initialDate = (taskData as? DateTimeTaskData)?.timeInMillis,
      onDateSelected = onDateSelected,
      onClear = {
        onResponseCleared()
        showSheet = false
      },
      onDismiss = { showSheet = false },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectionBottomSheet(
  initialDate: Long?,
  onDateSelected: (Date) -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      DatePicker(state = datePickerState)
      Spacer(modifier = Modifier.height(16.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { onDateSelected(Date(it)) }
            onDismiss()
          }
        ) {
          Text(stringResource(android.R.string.ok))
        }
      }
    }
  }
}
