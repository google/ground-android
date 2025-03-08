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
import android.content.DialogInterface
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.groundplatform.android.R
import org.groundplatform.android.databinding.DateTaskFragBinding
import org.groundplatform.android.model.submission.DateTimeTaskData
import org.groundplatform.android.ui.datacollection.components.TaskView
import org.groundplatform.android.ui.datacollection.components.TaskViewFactory
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.jetbrains.annotations.TestOnly

@AndroidEntryPoint
class DateTaskFragment : AbstractTaskFragment<DateTaskViewModel>() {

  private var datePickerDialog: DatePickerDialog? = null

  lateinit var dateText: LiveData<String>
  lateinit var dateTextHint: LiveData<String>

  override fun onTaskViewAttached() {
    super.onTaskViewAttached()
    dateText =
      viewModel.taskTaskData
        .filterIsInstance<DateTimeTaskData?>()
        .map { taskData ->
          if (taskData != null) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = taskData.timeInMillis
            DateFormat.getDateFormat(requireContext()).format(calendar.time)
          } else {
            ""
          }
        }
        .asLiveData()

    dateTextHint =
      MutableLiveData<String>().apply {
        val dateFormat = DateFormat.getDateFormat(requireContext()) as SimpleDateFormat
        val pattern = dateFormat.toPattern()
        val hint = pattern.uppercase()
        value = hint
      }
  }

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val taskBinding = DateTaskFragBinding.inflate(inflater)
    taskBinding.lifecycleOwner = this
    taskBinding.fragment = this
    return taskBinding.root
  }

  fun showDateDialog() {
    val calendar = Calendar.getInstance()
    val year = calendar[Calendar.YEAR]
    val month = calendar[Calendar.MONTH]
    val day = calendar[Calendar.DAY_OF_MONTH]
    datePickerDialog =
      DatePickerDialog(
        requireContext(),
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
    datePickerDialog?.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.clear)) { _, _ ->
      viewModel.clearResponse()
    }
    datePickerDialog?.show()
  }

  @TestOnly fun getDatePickerDialog(): DatePickerDialog? = datePickerDialog
}
