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
package com.google.android.ground.ui.tos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.google.android.ground.R
import com.google.android.ground.databinding.FragmentTermsServiceBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.EphemeralPopups
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(AbstractFragment::class)
class TermsOfServiceFragment : Hilt_TermsOfServiceFragment(), BackPressListener {

  @Inject lateinit var popups: EphemeralPopups

  private val viewModel: TermsOfServiceViewModel by hiltNavGraphViewModels(R.id.navGraph)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding = FragmentTermsServiceBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.termsOfServiceText = getTermsOfServiceText()
    binding.lifecycleOwner = this
    return binding.root
  }

  private fun getTermsOfServiceText(): String =
    TermsOfServiceFragmentArgs.fromBundle(requireNotNull(arguments)).termsOfServiceText.orEmpty()

  override fun onBack(): Boolean {
    requireActivity().finish()
    return false
  }
}
