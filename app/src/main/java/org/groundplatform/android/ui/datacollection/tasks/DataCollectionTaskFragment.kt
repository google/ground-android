/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.LoiNameAction
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.TaskMapFragmentContainer
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskMapFragment
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskMapFragment
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskMapFragment
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskViewModel
import org.groundplatform.android.ui.home.HomeScreenViewModel
import org.groundplatform.android.util.createComposeView
import org.groundplatform.android.util.openAppSettings
import javax.inject.Inject
import javax.inject.Provider

/**
 * A generic fragment that hosts the Compose view for data collection tasks.
 *
 * This fragment dynamically renders the appropriate task screen (e.g., text input, multiple choice,
 * photo, location capture) based on the provided task ID. It also manages the lifecycle of
 * task-specific map fragments when required by the task type.
 */
@AndroidEntryPoint
class DataCollectionTaskFragment @Inject constructor() : AbstractFragment() {
  @Inject
  lateinit var captureLocationTaskMapFragmentProvider: Provider<CaptureLocationTaskMapFragment>

  @Inject
  lateinit var dropPinTaskMapFragmentProvider: Provider<DropPinTaskMapFragment>

  @Inject
  lateinit var drawAreaTaskMapFragmentProvider: Provider<DrawAreaTaskMapFragment>

  private val dataCollectionViewModel: DataCollectionViewModel by hiltNavGraphViewModels(R.id.data_collection)

  private val homeScreenViewModel: HomeScreenViewModel by lazy {
    getViewModel(HomeScreenViewModel::class.java)
  }

  private val taskId: String by lazy {
    arguments?.getString(TASK_ID) ?: error("taskId not found in arguments")
  }

  private val viewModel: AbstractTaskViewModel by lazy {
    dataCollectionViewModel.getTaskViewModel(taskId)
      ?: error("ViewModel for taskId:$taskId not found.")
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ) = createComposeView {
    val loiName by dataCollectionViewModel.loiNameDraft.collectAsStateWithLifecycle()
    val showLoiNameDialog by dataCollectionViewModel.loiNameDialogOpen

    val onButtonClicked = { action: ButtonAction ->
      viewModel.onButtonClick(action)
    }

    val onFooterPositionUpdated = { top: Float ->
      dataCollectionViewModel.updateFooterPosition(taskId, top)
    }

    val onLoiNameAction = { action: LoiNameAction ->
      dataCollectionViewModel.handleLoiNameAction(action, taskId)
    }

    when (val vm = viewModel) {
      is CaptureLocationTaskViewModel ->
        CaptureLocationTaskScreen(
          viewModel = vm,
          onButtonClicked = onButtonClicked,
          onFooterPositionUpdated = onFooterPositionUpdated,
          onOpenSettings = { requireActivity().openAppSettings() },
        ) {
          TaskMapFragmentContainer(
            taskId = taskId,
            fragmentManager = childFragmentManager,
            fragmentProvider = captureLocationTaskMapFragmentProvider,
          )
        }

      is DateTaskViewModel -> DateTaskScreen(
        viewModel = vm,
        onButtonClicked = onButtonClicked,
        onFooterPositionUpdated = onFooterPositionUpdated,
      )

      is DrawAreaTaskViewModel ->
        DrawAreaTaskScreen(
          viewModel = vm,
          onButtonClicked = onButtonClicked,
          onFooterPositionUpdated = onFooterPositionUpdated,
          onLoiNameAction = onLoiNameAction,
          loiName = loiName,
          shouldShowLoiNameDialog = showLoiNameDialog,
        ) {
          TaskMapFragmentContainer(
            taskId = taskId,
            fragmentManager = childFragmentManager,
            fragmentProvider = drawAreaTaskMapFragmentProvider,
          )
        }

      is DropPinTaskViewModel ->
        DropPinTaskScreen(
          viewModel = vm,
          onButtonClicked = onButtonClicked,
          onFooterPositionUpdated = onFooterPositionUpdated,
          onLoiNameAction = onLoiNameAction,
          loiName = loiName,
          shouldShowLoiNameDialog = showLoiNameDialog,
        ) {
          TaskMapFragmentContainer(
            taskId = taskId,
            fragmentManager = childFragmentManager,
            fragmentProvider = dropPinTaskMapFragmentProvider,
          )
        }

      is InstructionTaskViewModel -> InstructionTaskScreen(
        viewModel = vm,
        onButtonClicked = onButtonClicked,
        onFooterPositionUpdated = onFooterPositionUpdated,
      )

      is MultipleChoiceTaskViewModel -> MultipleChoiceTaskScreen(
        viewModel = vm,
        onButtonClicked = onButtonClicked,
        onFooterPositionUpdated = onFooterPositionUpdated,
      )

      is PhotoTaskViewModel ->
        PhotoTaskScreen(
          viewModel = vm,
          onButtonClicked = onButtonClicked,
          onFooterPositionUpdated = onFooterPositionUpdated,
          onAwaitingPhotoCapture = { homeScreenViewModel.awaitingPhotoCapture = it },
        )

      is NumberTaskViewModel -> NumberTaskScreen(
        viewModel = vm,
        onButtonClicked = onButtonClicked,
        onFooterPositionUpdated = onFooterPositionUpdated,
      )

      is TextTaskViewModel -> TextTaskScreen(
        viewModel = vm,
        onButtonClicked = onButtonClicked,
        onFooterPositionUpdated = onFooterPositionUpdated,
      )

      is TimeTaskViewModel -> TimeTaskScreen(
        viewModel = vm,
        onButtonClicked = onButtonClicked,
        onFooterPositionUpdated = onFooterPositionUpdated,
      )

      else -> error("Unsupported task type: ${viewModel.task.type}")
    }
  }

  companion object {
    const val TASK_ID = "taskId"
  }
}
