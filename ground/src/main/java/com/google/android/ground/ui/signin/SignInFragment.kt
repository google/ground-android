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
package com.google.android.ground.ui.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.databinding.SignInFragBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import dagger.hilt.android.AndroidEntryPoint

// TODO: Remove scoping since Fragments are no longer injected.
@AndroidEntryPoint
class SignInFragment : AbstractFragment(), BackPressListener {

  private lateinit var viewModel: SignInViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SignInViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding = SignInFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onBack(): Boolean {
    // Workaround to exit on back from sign-in screen since for some reason
    // popUpTo is not working on signOut action.
    requireActivity().finish()
    return false
  }
}
