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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.google.android.ground.MainActivity
import com.google.android.ground.R
import com.google.android.ground.databinding.SurveySelectorFragBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import dagger.hilt.android.AndroidEntryPoint

/** User interface implementation of survey selector screen. */
@AndroidEntryPoint
class SurveySelectorFragment : AbstractFragment(), BackPressListener {

  private lateinit var viewModel: SurveySelectorViewModel
  private lateinit var binding: SurveySelectorFragBinding
  private lateinit var adapter: SurveyListAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SurveySelectorViewModel::class.java)
    adapter = SurveyListAdapter(viewModel, this)
    viewModel.surveySummaries.observe(this) { adapter.updateData(it) }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = SurveySelectorFragBinding.inflate(inflater, container, false)
    binding.lifecycleOwner = this

    // Required for unit tests since they are run inside a sandbox activity
    if (requireActivity() is MainActivity) {
      getMainActivity().setActionBar(binding.toolbar, true)
    }

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.recyclerView.adapter = adapter
  }

  fun showPopupMenu(view: View, surveyId: String) {
    with(PopupMenu(requireContext(), view)) {
      inflate(R.menu.survey_options_menu)
      setOnMenuItemClickListener(
        object : PopupMenu.OnMenuItemClickListener {
          override fun onMenuItemClick(item: MenuItem): Boolean {
            if (item.itemId == R.id.delete) {
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
