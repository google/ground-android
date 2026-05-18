/*
 * Copyright 2022 Google LLC
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
package org.groundplatform.android.ui.datacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.home.HomeScreenViewModel
import org.groundplatform.android.util.createComposeView
import org.groundplatform.android.util.openAppSettings
import javax.inject.Inject

/** Fragment allowing the user to collect data to complete a task. */
@AndroidEntryPoint
class DataCollectionFragment : AbstractFragment(), BackPressListener {
  @Inject lateinit var popups: EphemeralPopups

  val viewModel: DataCollectionViewModel by hiltNavGraphViewModels(R.id.data_collection)

  val homeScreenViewModel: HomeScreenViewModel by lazy {
    getViewModel(HomeScreenViewModel::class.java)
  }

  private var isNavigatingUp = false

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = createComposeView {
    DataCollectionScreen(
      viewModel = viewModel,
      onValidationError = { resId -> popups.ErrorPopup().show(resId) },
      onExitConfirmed = { navigateBack() },
      onOpenSettings = { requireActivity().openAppSettings() },
      onAwaitingPhotoCapture = { homeScreenViewModel.awaitingPhotoCapture = it },
    )
  }

  override fun onResume() {
    super.onResume()
    isNavigatingUp = false
  }

  override fun onPause() {
    super.onPause()
    if (!isNavigatingUp) {
      viewModel.saveCurrentState()
    }
  }

  override fun onBack(): Boolean {
    if (viewModel.uiState.value is DataCollectionUiState.TaskSubmitted) {
      // Pressing back button after submitting task should navigate back to home screen.
      navigateBack()
      return true
    }

    if (viewModel.isAtFirstTask()) {
      viewModel.showExitWarning()
    } else {
      viewModel.moveToPreviousTask()
    }
    return true
  }

  private fun navigateBack() {
    isNavigatingUp = true
    viewModel.clearDraftBlocking()
    findNavController().navigateUp()
  }

  companion object {
    const val TASK_ID: String = "taskId"
  }
}
