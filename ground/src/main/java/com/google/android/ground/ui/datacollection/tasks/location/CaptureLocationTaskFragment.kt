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
package com.google.android.ground.ui.datacollection.tasks.location

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.databinding.CaptureLocationTaskFragBinding
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint(AbstractTaskFragment::class)
class CaptureLocationTaskFragment :
  Hilt_CaptureLocationTaskFragment<CaptureLocationTaskViewModel>() {
  override fun onCreateTaskView(inflater: LayoutInflater, container: ViewGroup?): TaskView =
    TaskViewFactory.createWithCombinedHeader(
      inflater,
      R.drawable.outline_pin_drop,
      R.string.capture_location
    )

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val taskBinding = CaptureLocationTaskFragBinding.inflate(inflater)
    taskBinding.lifecycleOwner = this
    taskBinding.viewModel = viewModel
    return taskBinding.root
  }

  override fun onCreateActionButtons() {
    addSkipButton()
    addUndoButton().setOnClickListener {
      viewModel.clearResponse()
      enableLocationUpdates()
    }
    addButton(ButtonAction.CAPTURE_LOCATION)
      .setOnClickListener {
        disableLocationUpdates()
        viewModel.updateResponse()
      }
      .setOnTaskUpdated { button, taskData -> button.showIfTrue(taskData.isNullOrEmpty()) }
    addButton(ButtonAction.CONTINUE)
      .setOnClickListener {
        disableLocationUpdates()
        dataCollectionViewModel.onContinueClicked()
      }
      .setOnTaskUpdated { button, taskData -> button.showIfTrue(taskData.isNotNullOrEmpty()) }
      .hide()
  }

  private fun enableLocationUpdates() {
    viewLifecycleOwner.lifecycleScope.launch { viewModel.enableLocationUpdates() }
  }

  private fun disableLocationUpdates() {
    viewLifecycleOwner.lifecycleScope.launch { viewModel.disableLocationUpdates() }
  }

  override fun onResume() {
    super.onResume()
    enableLocationUpdates()
  }

  override fun onPause() {
    disableLocationUpdates()
    super.onPause()
  }
}
