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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import org.groundplatform.android.R
import org.groundplatform.android.model.submission.isNullOrEmpty
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
    addButton(ButtonAction.DROP_PIN)
      .setOnClickListener { viewModel.dropPin() }
      .setOnValueChanged { button, value -> button.showIfTrue(value.isNullOrEmpty()) }
    addNextButton(hideIfEmpty = true)
  }

  override fun onTaskResume() {
    if (isVisible && !viewModel.instructionsDialogShown) {
      showInstructionsDialog()
    }
  }

  private fun showInstructionsDialog() {
    viewModel.instructionsDialogShown = true
    renderComposableDialog {
      InstructionsDialog(iconId = R.drawable.swipe_24, stringId = R.string.drop_a_pin_tooltip_text)
    }
  }
}
