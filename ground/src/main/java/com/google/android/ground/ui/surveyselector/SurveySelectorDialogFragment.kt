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

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.google.android.ground.R
import com.google.android.ground.databinding.SurveySelectorDialogBinding
import com.google.android.ground.model.Survey
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Loadable.LoadState
import com.google.android.ground.ui.common.AbstractDialogFragment
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.common.base.Preconditions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/** User interface implementation of survey selector dialog. */
@AndroidEntryPoint
class SurveySelectorDialogFragment : AbstractDialogFragment() {

  @Inject lateinit var popups: EphemeralPopups

  private lateinit var viewModel: SurveySelectorViewModel
  private lateinit var binding: SurveySelectorDialogBinding
  private lateinit var listAdapter: ArrayAdapter<Any>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SurveySelectorViewModel::class.java)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    super.onCreateDialog(savedInstanceState)
    val dialog = AlertDialog.Builder(requireContext())
    dialog.setTitle(R.string.join_survey)
    binding = SurveySelectorDialogBinding.inflate(layoutInflater)
    listAdapter =
      ArrayAdapter<Any>(requireContext(), R.layout.survey_selector_list_item, R.id.survey_name)
    binding.surveySelectorListView.adapter = listAdapter
    viewModel.surveySummaries.observe(this) { updateSurveyList(it) }
    binding.surveySelectorListView.onItemClickListener =
      OnItemClickListener { _: AdapterView<*>?, _: View?, index: Int, _: Long ->
        onItemSelected(index)
      }
    dialog.setView(binding.root)
    dialog.setCancelable(false)
    return dialog.create()
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
    dismiss()
  }

  private fun showSurveyList(list: List<Survey>) {
    binding.listLoadingProgressBar.visibility = View.GONE
    Preconditions.checkNotNull(
      listAdapter,
      "listAdapter was null when attempting to show survey list"
    )
    listAdapter.clear()
    list.map(Survey::title).forEach { listAdapter.add(it) }
    binding.surveySelectorListView.visibility = View.VISIBLE
  }

  private fun onItemSelected(index: Int) {
    dismiss()
    viewModel.activateSurvey(index)
  }
}
