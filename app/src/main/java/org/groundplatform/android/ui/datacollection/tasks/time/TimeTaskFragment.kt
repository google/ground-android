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
package org.groundplatform.android.ui.datacollection.tasks.time

import android.app.TimePickerDialog
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
import org.groundplatform.android.databinding.TimeTaskFragBinding
import org.groundplatform.android.model.submission.DateTimeTaskData
import org.groundplatform.android.ui.datacollection.components.TaskView
import org.groundplatform.android.ui.datacollection.components.TaskViewFactory
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.jetbrains.annotations.TestOnly

@AndroidEntryPoint
class TimeTaskFragment : AbstractTaskFragment<TimeTaskViewModel>() {

  private var timePickerDialog: TimePickerDialog? = null

  lateinit var timeText: LiveData<String>
  lateinit var timeTextHint: LiveData<String>

  override fun onTaskViewAttached() {
    super.onTaskViewAttached()
    timeText =
      viewModel.taskTaskData
        .filterIsInstance<DateTimeTaskData?>()
        .map { taskData ->
          if (taskData != null) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = taskData.timeInMillis
            DateFormat.getTimeFormat(requireContext()).format(calendar.time)
          } else {
            ""
          }
        }
        .asLiveData()

    timeTextHint =
      MutableLiveData<String>().apply {
        val timeFormat = DateFormat.getTimeFormat(requireContext())
        val hint =
          if (timeFormat is SimpleDateFormat) {
            timeFormat.toPattern().uppercase()
          } else {
            "HH:MM AM/PM" // Fallback hint if DateFormat is not SimpleDateFormat
          }
        value = hint
      }
  }

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val taskBinding = TimeTaskFragBinding.inflate(inflater)
    taskBinding.lifecycleOwner = this
    taskBinding.fragment = this
    return taskBinding.root
  }

  fun showTimeDialog() {
    val calendar = Calendar.getInstance()
    val hour = calendar[Calendar.HOUR]
    val minute = calendar[Calendar.MINUTE]
    timePickerDialog =
      TimePickerDialog(
        requireContext(),
        { _, updatedHourOfDay, updatedMinute ->
          val c = Calendar.getInstance()
          c[Calendar.HOUR_OF_DAY] = updatedHourOfDay
          c[Calendar.MINUTE] = updatedMinute
          viewModel.updateResponse(c.time)
        },
        hour,
        minute,
        DateFormat.is24HourFormat(requireContext()),
      )
    timePickerDialog?.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.clear)) { _, _ ->
      viewModel.clearResponse()
    }
    timePickerDialog?.show()
  }

  @TestOnly fun getTimePickerDialog(): TimePickerDialog? = timePickerDialog
}
