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
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.BR
import com.google.android.ground.databinding.TimeTaskFragBinding
import com.google.android.ground.ui.datacollection.components.TaskViewWithHeader
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class TimeTaskFragment : AbstractTaskFragment<TimeTaskViewModel>() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    // Base template with header and footer
    taskView = TaskViewWithHeader.create(container, inflater, this, viewModel)

    // Task view
    val taskBinding = TimeTaskFragBinding.inflate(inflater, container, false)
    taskBinding.lifecycleOwner = this
    taskBinding.setVariable(BR.viewModel, viewModel)
    taskBinding.setVariable(BR.fragment, this)
    taskView.addTaskView(taskBinding.root)

    return taskView.root
  }

  fun showTimeDialog() {
    val calendar = Calendar.getInstance()
    val hour = calendar[Calendar.HOUR]
    val minute = calendar[Calendar.MINUTE]
    val timePickerDialog =
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
        DateFormat.is24HourFormat(requireContext())
      )
    timePickerDialog.show()
  }
}
