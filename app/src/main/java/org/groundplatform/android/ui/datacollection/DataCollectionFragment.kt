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

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.compose.runtime.getValue
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.databinding.DataCollectionFragBinding
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.home.HomeScreenFragmentDirections
import org.groundplatform.android.util.renderComposableDialog

/** Fragment allowing the user to collect data to complete a task. */
@AndroidEntryPoint
class DataCollectionFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var taskFragmentProvider: TaskFragmentProvider

  val viewModel: DataCollectionViewModel by hiltNavGraphViewModels(R.id.data_collection)

  private lateinit var binding: DataCollectionFragBinding
  private lateinit var progressBar: ProgressBar
  private lateinit var guideline: Guideline
  private var isNavigatingUp = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      // Clean up child fragments to prevent "No view found for id" exception on rotation
      childFragmentManager.fragments.forEach {
        childFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = DataCollectionFragBinding.inflate(inflater, container, false)
    progressBar = binding.progressBar
    guideline = binding.progressBarGuideline
    getAbstractActivity().setSupportActionBar(binding.dataCollectionToolbar)

    binding.dataCollectionToolbar.setNavigationOnClickListener { showExitWarningDialog() }

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.lifecycleOwner = viewLifecycleOwner

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.footerVerticalPosition.collect { setProgressBarPosition(it) }
      }
    }

    // Collect UI state safely across the Fragment view lifecycle.
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { ui -> updateUI(ui) }
      }
    }

    binding.composeView.setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      if (uiState is DataCollectionUiState.Ready) {
        TaskPager(
          tasks = (uiState as DataCollectionUiState.Ready).tasks,
          taskPosition = (uiState as DataCollectionUiState.Ready).position,
          fragmentManager = childFragmentManager,
          taskFragmentProvider = taskFragmentProvider,
        )
      }
    }
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

  private fun updateUI(uiState: DataCollectionUiState) {
    when (uiState) {
      is DataCollectionUiState.Ready -> {
        binding.jobName = uiState.job.name
        binding.loiName = uiState.loiName
        updateProgressBar(uiState.position)
      }

      is DataCollectionUiState.TaskUpdated -> {
        updateProgressBar(uiState.position)
      }

      is DataCollectionUiState.TaskSubmitted -> {
        onTaskSubmitted()
      }

      is DataCollectionUiState.Loading,
      is DataCollectionUiState.Error -> {
        // TODO: add loading and error support as per ui
      }
    }
  }

  private fun setProgressBarPosition(topPosition: Float) {
    val insets = requireView().rootWindowInsets ?: return
    val windowInsets = WindowInsetsCompat.toWindowInsetsCompat(insets)
    val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

    val guidelineTop = topPosition.toInt() - systemBarsInsets.top

    if (guidelineTop > 0) {
      guideline.setGuidelineBegin(guidelineTop)
    }
  }

  private fun onTaskSubmitted() {
    // Hide close button
    binding.dataCollectionToolbar.navigationIcon = null

    // Display a confirmation dialog and move to home screen after that.
    renderComposableDialog {
      DataSubmissionConfirmationScreen {
        findNavController().navigate(HomeScreenFragmentDirections.showHomeScreen())
      }
    }
  }

  private fun updateProgressBar(taskPosition: TaskPosition) {
    // Reset progress bar
    progressBar.max = (taskPosition.sequenceSize - 1) * PROGRESS_SCALE

    val target = taskPosition.relativeIndex * PROGRESS_SCALE
    progressBar.clearAnimation()
    ValueAnimator.ofInt(progressBar.progress, target)
      .apply {
        duration = 400L
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener { progressBar.progress = it.animatedValue as Int }
      }
      .start()
  }

  override fun onBack(): Boolean {
    if (viewModel.uiState.value == DataCollectionUiState.TaskSubmitted) {
      // Pressing back button after submitting task should navigate back to home screen.
      navigateBack()
      return true
    }

    if (viewModel.isAtFirstTask()) {
      showExitWarningDialog()
    } else {
      viewModel.moveToPreviousTask()
    }
    return true
  }

  private fun showExitWarningDialog() {
    renderComposableDialog {
      ConfirmationDialog(
        title = R.string.data_collection_cancellation_title,
        description = R.string.data_collection_cancellation_description,
        confirmButtonText = R.string.data_collection_cancellation_confirm_button,
        onConfirmClicked = { navigateBack() },
      )
    }
  }

  private fun navigateBack() {
    isNavigatingUp = true
    viewModel.clearDraftBlocking()
    findNavController().navigateUp()
  }

  private companion object {
    private const val PROGRESS_SCALE = 100
  }
}
