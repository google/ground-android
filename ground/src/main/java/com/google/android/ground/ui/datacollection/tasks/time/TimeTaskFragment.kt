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
package com.google.android.ground.ui.datacollection.tasks.time

import android.app.TimePickerDialog
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.databinding.TimeTaskFragBinding
import com.google.android.ground.model.submission.DateTimeTaskData
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.jetbrains.annotations.TestOnly

@AndroidEntryPoint
class TimeTaskFragment : AbstractTaskFragment<TimeTaskViewModel>() {

  private var timePickerDialog: TimePickerDialog? = null

  lateinit var timeText: LiveData<String>

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
      .apply {
        show()
        timePickerDialog = this
      }
  }

  @TestOnly fun getTimePickerDialog(): TimePickerDialog? = timePickerDialog
}
