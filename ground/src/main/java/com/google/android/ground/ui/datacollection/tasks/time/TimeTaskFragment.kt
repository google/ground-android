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
import androidx.fragment.app.activityViewModels
import com.google.android.ground.BR
import com.google.android.ground.databinding.TimeTaskFragBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.tasks.TaskFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.properties.Delegates

@AndroidEntryPoint
class TimeTaskFragment : AbstractFragment(), TaskFragment<TimeTaskViewModel> {
  private val dataCollectionViewModel: DataCollectionViewModel by activityViewModels()
  override lateinit var viewModel: TimeTaskViewModel
  override var position by Delegates.notNull<Int>()
  private lateinit var binding: TimeTaskFragBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      position = savedInstanceState.getInt(TaskFragment.POSITION)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(TaskFragment.POSITION, position)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = TimeTaskFragBinding.inflate(inflater, container, false)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel = dataCollectionViewModel.getTaskViewModel(position) as TimeTaskViewModel
    binding.lifecycleOwner = this
    binding.setVariable(BR.viewModel, viewModel)
    binding.setVariable(BR.fragment, this)
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
