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

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.text.format.DateFormat
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Calendar
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

@Composable
internal fun DateTaskContent(
  taskData: TaskData?,
  onDateSelected: (Date) -> Unit,
  onResponseCleared: () -> Unit,
) {
  val context = LocalContext.current

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
    onDateClick = {
      showDateDialog(
        context,
        onDateSelected = onDateSelected,
        onResponseCleared = onResponseCleared,
      )
    },
  )
}

// TODO: Replace with compose-based bottom modal date picker.
private fun showDateDialog(
  context: Context,
  onDateSelected: (Date) -> Unit,
  onResponseCleared: () -> Unit,
) {
  val calendar = Calendar.getInstance()
  val year = calendar[Calendar.YEAR]
  val month = calendar[Calendar.MONTH]
  val day = calendar[Calendar.DAY_OF_MONTH]
  val dialog =
    DatePickerDialog(
      context,
      { _, updatedYear, updatedMonth, updatedDayOfMonth ->
        val c = Calendar.getInstance()
        c[Calendar.DAY_OF_MONTH] = updatedDayOfMonth
        c[Calendar.MONTH] = updatedMonth
        c[Calendar.YEAR] = updatedYear
        onDateSelected(c.time)
      },
      year,
      month,
      day,
    )
  dialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.clear)) { _, _ ->
    onResponseCleared()
  }
  dialog.show()
}
