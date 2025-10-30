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
package org.groundplatform.android.ui.datacollection.tasks.point

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.model.submission.isNullOrEmpty
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.TaskView
import org.groundplatform.android.ui.datacollection.components.TaskViewFactory
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment.Companion.TASK_ID_FRAGMENT_ARG_KEY
import org.groundplatform.android.util.renderComposableDialog

@AndroidEntryPoint
class DropPinTaskFragment @Inject constructor() : AbstractTaskFragment<DropPinTaskViewModel>() {
  @Inject lateinit var dropPinTaskMapFragmentProvider: Provider<DropPinTaskMapFragment>

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithCombinedHeader(inflater, R.drawable.outline_pin_drop)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    // NOTE(#2493): Multiplying by a random prime to allow for some mathematical "uniqueness".
    // Otherwise, the sequentially generated ID might conflict with an ID produced by Google Maps.
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() * 11617 }
    val fragment = dropPinTaskMapFragmentProvider.get()
    val args = Bundle()
    args.putString(TASK_ID_FRAGMENT_ARG_KEY, taskId)
    fragment.arguments = args
    childFragmentManager
      .beginTransaction()
      .add(rowLayout.id, fragment, "Drop a pin fragment")
      .commit()
    return rowLayout
  }

  override fun onCreateActionButtons() {
    addSkipButton()
    addUndoButton()

    // Show different buttons based on task configuration
    if (viewModel.task.allowMovingPoint) {
      // Mode: Allow dropping pin anywhere on map
      addButton(ButtonAction.DROP_PIN)
        .setOnClickListener { viewModel.dropPin() }
        .setOnValueChanged { button, value -> button.showIfTrue(value.isNullOrEmpty()) }
    } else {
      // Mode: GPS location only
      addButton(ButtonAction.CAPTURE_LOCATION)
        .setOnClickListener { viewModel.captureLocation() }
        .setOnValueChanged { button, value -> button.showIfTrue(value.isNullOrEmpty()) }
    }

    addNextButton(hideIfEmpty = true)
  }

  override fun onTaskResume() {
    if (isVisible) {
      // Show instructions dialog if not shown yet
      if (!viewModel.instructionsDialogShown) {
        showInstructionsDialog()
      }

      // For GPS-only mode, ensure location lock is enabled
      if (!viewModel.task.allowMovingPoint) {
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
  }

  private fun showInstructionsDialog() {
    viewModel.instructionsDialogShown = true
    renderComposableDialog {
      if (viewModel.task.allowMovingPoint) {
        // Show instructions for drop pin mode
        InstructionsDialog(
          iconId = R.drawable.swipe_24,
          stringId = R.string.add_point_tooltip_text
        )
      } else {
        // Show instructions for GPS capture mode  
        InstructionsDialog(
          iconId = R.drawable.swipe_24,
          stringId = R.string.capture_location_tooltip_text
        )
      }
    }
  }

  private fun showLocationPermissionDialog() {
    renderComposableDialog {
      ConfirmationDialog(
        title = R.string.allow_location_title,
        description = R.string.allow_location_description,
        confirmButtonText = R.string.allow_location_confirmation,
        onConfirmClicked = {
          // Open the app settings
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          intent.data = Uri.fromParts("package", context?.packageName, null)
          context?.startActivity(intent)
        },
      )
    }
  }
}
