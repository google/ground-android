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
package com.google.android.ground.ui.datacollection.tasks.point

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.google.android.ground.R
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.InstructionsDialog
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DropPinTaskFragment : AbstractTaskFragment<DropPinTaskViewModel>() {

  @Inject lateinit var map: MapFragment

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithCombinedHeader(inflater, R.drawable.outline_pin_drop)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    // NOTE(#2493): Multiplying by a random prime to allow for some mathematical "uniqueness".
    // Otherwise, the sequentially generated ID might conflict with an ID produced by Google Maps.
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() * 11617 }
    parentFragmentManager
      .beginTransaction()
      .add(rowLayout.id, DropPinTaskMapFragment.newInstance(viewModel, map), "Drop a pin fragment")
      .commit()
    return rowLayout
  }

  override fun onCreateActionButtons() {
    addSkipButton()
    addUndoButton()
    addButton(ButtonAction.DROP_PIN)
      .setOnClickListener { viewModel.dropPin() }
      .setOnValueChanged { button, value -> button.showIfTrue(value.isNullOrEmpty()) }
    addButton(ButtonAction.NEXT)
      .setOnClickListener { handleNext() }
      .setOnValueChanged { button, value ->
        button.showIfTrue(value.isNotNullOrEmpty())
        button.toggleDone(checkLastPositionWithTaskData(value))
      }
      .hide()
  }

  override fun onTaskResume() {
    if (isVisible && !viewModel.instructionsDialogShown) {
      showInstructionsDialog()
    }
  }

  private fun showInstructionsDialog() {
    viewModel.instructionsDialogShown = true
    (view as ViewGroup).addView(
      ComposeView(requireContext()).apply {
        setContent {
          val openAlertDialog = remember { mutableStateOf(true) }
          when {
            openAlertDialog.value -> {
              AppTheme {
                InstructionsDialog(R.drawable.swipe_24, R.string.drop_a_pin_tooltip_text) {
                  openAlertDialog.value = false
                }
              }
            }
          }
        }
      }
    )
  }
}
