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
import org.groundplatform.android.model.submission.DateTimeTaskData
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.tasks.TaskContainer
import org.groundplatform.ui.theme.sizes

@Composable
fun DateTaskScreen(viewModel: DateTaskViewModel, dataCollectionViewModel: DataCollectionViewModel) {
  val taskData by viewModel.taskTaskData.collectAsStateWithLifecycle()
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

  TaskContainer(viewModel = viewModel, dataCollectionViewModel = dataCollectionViewModel) {
    DateTaskScreen(
      modifier = Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding),
      dateText = dateText,
      hintText = hintText,
      onDateClick = { showDateDialog(context, viewModel) },
    )
  }
}

// TODO: Replace with bottom modal date picker.
private fun showDateDialog(context: Context, viewModel: DateTaskViewModel) {
  val calendar = Calendar.getInstance()
  val year = calendar[Calendar.YEAR]
  val month = calendar[Calendar.MONTH]
  val day = calendar[Calendar.DAY_OF_MONTH]
  val datePickerDialog =
    DatePickerDialog(
      context,
      { _, updatedYear, updatedMonth, updatedDayOfMonth ->
        val c = Calendar.getInstance()
        c[Calendar.DAY_OF_MONTH] = updatedDayOfMonth
        c[Calendar.MONTH] = updatedMonth
        c[Calendar.YEAR] = updatedYear
        viewModel.updateResponse(c.time)
      },
      year,
      month,
      day,
    )
  datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.clear)) {
    _,
    _ ->
    viewModel.clearResponse()
  }
  datePickerDialog.show()
}
