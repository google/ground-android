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
package com.google.android.ground.ui.surveyselector

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.navigation.fragment.findNavController
import com.google.android.ground.R
import com.google.android.ground.databinding.SurveySelectorFragBinding
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.util.visibleIf
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** User interface implementation of survey selector screen. */
@AndroidEntryPoint
class SurveySelectorFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  private lateinit var viewModel: SurveySelectorViewModel
  private lateinit var binding: SurveySelectorFragBinding
  private lateinit var adapter: SurveyListAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SurveySelectorViewModel::class.java)
    adapter = SurveyListAdapter(viewModel, this)
    viewModel.uiState.launchWhenStartedAndCollect { updateUi(it) }
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
    adapter.updateData(surveys)
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
    binding.recyclerView.adapter = adapter
    getAbstractActivity().setSupportActionBar(binding.toolbar)

    // TODO - https://github.com/google/ground-android/issues/2692#issuecomment-2430978043
    if (parentFragmentManager.backStackEntryCount > 0) {
      getAbstractActivity().supportActionBar?.setDisplayHomeAsUpEnabled(true)
    } else {
      getAbstractActivity().supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
  }

  fun showPopupMenu(view: View, surveyId: String) {
    with(PopupMenu(requireContext(), view)) {
      inflate(R.menu.survey_options_menu)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        setForceShowIcon(true)
      }
      setOnMenuItemClickListener(
        object : PopupMenu.OnMenuItemClickListener {
          override fun onMenuItemClick(item: MenuItem): Boolean {
            if (item.itemId == R.id.remove_offline_access_menu_item) {
              viewModel.deleteSurvey(surveyId)
              return true
            }
            return false
          }
        }
      )
      show()
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
