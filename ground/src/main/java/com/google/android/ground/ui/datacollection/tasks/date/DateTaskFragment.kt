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
package com.google.android.ground.ui.datacollection.tasks.date

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import com.google.android.ground.databinding.DateTaskFragBinding
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import org.jetbrains.annotations.TestOnly

@AndroidEntryPoint
class DateTaskFragment : AbstractTaskFragment<DateTaskViewModel>() {

  private var datePickerDialog: DatePickerDialog? = null

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val taskBinding = DateTaskFragBinding.inflate(inflater)
    taskBinding.lifecycleOwner = this
    taskBinding.fragment = this
    taskBinding.viewModel = viewModel
    return taskBinding.root
  }

  fun showDateDialog() {
    val calendar = Calendar.getInstance()
    val year = calendar[Calendar.YEAR]
    val month = calendar[Calendar.MONTH]
    val day = calendar[Calendar.DAY_OF_MONTH]
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
      .apply {
        show()
        datePickerDialog = this
      }
  }

  @TestOnly fun getDatePickerDialog(): DatePickerDialog? = datePickerDialog
}
