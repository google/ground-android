/*
 * Copyright 2018 Google LLC
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
package org.groundplatform.android.ui.surveyselector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.databinding.SurveySelectorFragBinding
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.home.HomeScreenFragmentDirections
import org.groundplatform.android.ui.theme.AppTheme
import org.groundplatform.android.util.visibleIf

/** User interface implementation of survey selector screen. */
@AndroidEntryPoint
class SurveySelectorFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  private lateinit var viewModel: SurveySelectorViewModel
  private lateinit var binding: SurveySelectorFragBinding

  private val args: SurveySelectorFragmentArgs by navArgs()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SurveySelectorViewModel::class.java)
    viewModel.uiState.launchWhenStartedAndCollect { updateUi(it) }
    if (!args.surveyId.isNullOrBlank()) {
      viewModel.activateSurvey(args.surveyId!!)
    }
  }

  private fun updateUi(uiState: UiState) {
    when (uiState) {
      UiState.ActivatingSurvey,
      UiState.FetchingSurveys -> {
        showProgressDialog()
      }
      UiState.SurveyActivated -> {
        if (!viewModel.surveyActivationInProgress) {
          dismissProgressDialog()
        }
      }
      is UiState.SurveyListAvailable -> {
        handleSurveyListUpdated(uiState.surveys)
        if (!viewModel.surveyActivationInProgress) {
          dismissProgressDialog()
        }
      }
      is UiState.Error -> {
        dismissProgressDialog()
        ephemeralPopups.ErrorPopup().unknownError()
      }
      is UiState.NavigateToHome -> {
        findNavController().navigate(HomeScreenFragmentDirections.showHomeScreen())
      }
    }
  }

  private fun handleSurveyListUpdated(surveys: List<SurveyListItem>) {
    with(binding) {
      container.visibleIf(surveys.isNotEmpty())
      emptyContainer.visibleIf(surveys.isEmpty())
    }
    // TODO: Migrate complete screen to compose
    binding.composeView.setContent { AppTheme { SurveyList(surveys, viewModel) } }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = SurveySelectorFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    getAbstractActivity().setSupportActionBar(binding.toolbar)

    if (parentFragmentManager.backStackEntryCount > 0) {
      getAbstractActivity().supportActionBar?.setDisplayHomeAsUpEnabled(true)
    } else {
      getAbstractActivity().supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
  }

  private fun shouldExitApp(): Boolean =
    arguments?.let { SurveySelectorFragmentArgs.fromBundle(it).shouldExitApp } ?: false

  override fun onBack(): Boolean {
    if (shouldExitApp()) {
      requireActivity().finish()
      return true
    }
    return false
  }
}
