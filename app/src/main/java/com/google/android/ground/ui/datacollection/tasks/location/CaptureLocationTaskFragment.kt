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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.ui.compose.ConfirmationDialog
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskMapFragment.Companion.TASK_ID_FRAGMENT_ARG_KEY
import com.google.android.ground.util.renderComposableDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CaptureLocationTaskFragment @Inject constructor() :
  AbstractTaskFragment<CaptureLocationTaskViewModel>() {
  @Inject
  lateinit var captureLocationTaskMapFragmentProvider: Provider<CaptureLocationTaskMapFragment>

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithCombinedHeader(inflater, R.drawable.outline_pin_drop)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    // NOTE(#2493): Multiplying by a random prime to allow for some mathematical uniqueness.
    // Otherwise, the sequentially generated ID might conflict with an ID produced by Google Maps.
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() * 11149 }
    val fragment = captureLocationTaskMapFragmentProvider.get()
    val args = Bundle()
    args.putString(TASK_ID_FRAGMENT_ARG_KEY, taskId)
    fragment.arguments = args
    childFragmentManager
      .beginTransaction()
      .add(rowLayout.id, fragment, CaptureLocationTaskMapFragment::class.java.simpleName)
      .commit()
    return rowLayout
  }

  override fun onTaskResume() {
    // Ensure that the location lock is enabled, if it hasn't been.
    if (isVisible) {
      viewModel.enableLocationLock()
      lifecycleScope.launch {
        viewModel.enableLocationLockFlow.collect {
          if (it == LocationLockEnabledState.NEEDS_ENABLE) {
            showLocationPermissionDialog()
          }
        }
      }
    }
  }

  override fun onCreateActionButtons() {
    addSkipButton()
    addUndoButton()
    addButton(ButtonAction.CAPTURE_LOCATION)
      .setOnClickListener { viewModel.updateResponse() }
      .setOnValueChanged { button, value -> button.showIfTrue(value.isNullOrEmpty()) }
    addNextButton(hideIfEmpty = true)
  }

  private fun showLocationPermissionDialog() {
    renderComposableDialog {
      ConfirmationDialog(
        title = R.string.allow_location_title,
        description = R.string.allow_location_description,
        confirmButtonText = R.string.allow_location_confirmation,
      ) {
        // Open the app settings
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context?.packageName, null)
        context?.startActivity(intent)
      }
    }
  }
}
