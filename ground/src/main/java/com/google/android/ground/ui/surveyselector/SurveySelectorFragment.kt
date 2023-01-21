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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.R
import com.google.android.ground.databinding.SurveySelectorFragBinding
import com.google.android.ground.model.Survey
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Loadable.LoadState
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.EphemeralPopups
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/** User interface implementation of survey selector screen. */
@AndroidEntryPoint
class SurveySelectorFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var popups: EphemeralPopups

  private lateinit var viewModel: SurveySelectorViewModel
  private lateinit var binding: SurveySelectorFragBinding
  private lateinit var adapter: SurveyListAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SurveySelectorViewModel::class.java)
    adapter = SurveyListAdapter(viewModel)

    viewModel.surveySummaries.observe(this) { updateSurveyList(it) }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = SurveySelectorFragBinding.inflate(inflater, container, false)
    binding.lifecycleOwner = this
    binding.viewModel = viewModel
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.recyclerView.adapter = adapter
  }

  private fun updateSurveyList(surveySummaries: Loadable<List<Survey>>) {
    when (surveySummaries.state) {
      LoadState.LOADING -> Timber.i("Loading surveys")
      LoadState.LOADED ->
        surveySummaries.value().ifPresent { list: List<Survey> -> showSurveyList(list) }
      LoadState.ERROR -> onSurveyListLoadError(surveySummaries.error().orElse(UnknownError()))
    }
  }

  private fun onSurveyListLoadError(t: Throwable) {
    Timber.e(t, "Survey list not available")
    popups.showError(R.string.survey_list_load_error)
  }

  private fun showSurveyList(surveys: List<Survey>) {
    surveys
      .map {
        SurveyItem(surveyId = it.id, surveyTitle = it.title, surveyDescription = it.description)
      }
      .toList()
      .apply { adapter.updateData(this) }
  }

  override fun onBack(): Boolean {
    requireActivity().finish()
    return false
  }
}
