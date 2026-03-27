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

import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskMapFragmentContainer
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment

@AndroidEntryPoint
class DropPinTaskFragment @Inject constructor() : AbstractTaskFragment<DropPinTaskViewModel>() {
  @Inject lateinit var dropPinTaskMapFragmentProvider: Provider<DropPinTaskMapFragment>

  override val taskHeader: TaskHeader by lazy {
    TaskHeader(viewModel.task.label, R.drawable.outline_pin_drop)
  }

  override val instructionData =
    InstructionData(iconId = R.drawable.swipe_24, stringId = R.string.drop_a_pin_tooltip_text)

  @Composable
  override fun TaskBody() {
    TaskMapFragmentContainer(
      taskId = viewModel.task.id,
      fragmentManager = childFragmentManager,
      fragmentProvider = dropPinTaskMapFragmentProvider,
    )
  }

  override fun onTaskResume() {
    if (isVisible && viewModel.shouldShowInstructionsDialog()) {
      viewModel.showInstructionsDialog.value = true
    }
  }

  override fun onInstructionDialogDismissed() {
    viewModel.instructionsDialogShown = true
  }
}
