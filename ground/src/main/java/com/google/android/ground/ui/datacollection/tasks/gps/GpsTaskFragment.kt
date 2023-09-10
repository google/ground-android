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
package com.google.android.ground.ui.datacollection.tasks.gps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.R
import com.google.android.ground.databinding.GpsTaskFragBinding
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(AbstractTaskFragment::class)
class GpsTaskFragment : Hilt_GpsTaskFragment<GpsTaskViewModel>() {
  override fun onCreateTaskView(inflater: LayoutInflater, container: ViewGroup?): TaskView =
    TaskViewFactory.createWithCombinedHeader(
      inflater,
      R.drawable.outline_pin_drop,
      R.string.capture_gps
    )

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val taskBinding = GpsTaskFragBinding.inflate(inflater)
    taskBinding.lifecycleOwner = this
    taskBinding.viewModel = viewModel
    return taskBinding.root
  }

  override fun onCreateActionButtons() {
    super.onCreateActionButtons()
    addUndoButton()
  }
}
