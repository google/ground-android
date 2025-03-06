/*
 * Copyright 2021 Google LLC
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
package org.groundplatform.android.ui.tos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.groundplatform.android.databinding.FragmentTermsServiceBinding
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.surveyselector.SurveySelectorFragmentDirections

@AndroidEntryPoint
class TermsOfServiceFragment : AbstractFragment() {

  private lateinit var viewModel: TermsOfServiceViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(TermsOfServiceViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val args = TermsOfServiceFragmentArgs.fromBundle(requireArguments())
    val binding = FragmentTermsServiceBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.isViewOnly = args.isViewOnly
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      viewModel.navigateToSurveySelector.collect {
        findNavController()
          .navigate(SurveySelectorFragmentDirections.showSurveySelectorScreen(true))
      }
    }
  }
}
