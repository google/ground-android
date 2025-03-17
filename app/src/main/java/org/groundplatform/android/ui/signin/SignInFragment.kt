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
package org.groundplatform.android.ui.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.databinding.SignInFragBinding
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener

@AndroidEntryPoint
class SignInFragment : AbstractFragment(), BackPressListener {

  private lateinit var binding: SignInFragBinding
  private lateinit var viewModel: SignInViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SignInViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = SignInFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    lifecycleScope.launch(Dispatchers.Main) {
      viewModel.getNetworkFlow().filterNotNull().collect { connected ->
        if (!connected) {
          displayNetworkError()
        }
      }
    }
  }

  private fun displayNetworkError() {
    Snackbar.make(
        requireView(),
        getString(R.string.network_error_when_signing_in),
        Snackbar.LENGTH_LONG,
      )
      .show()
  }

  override fun onBack(): Boolean {
    // Workaround to exit on back from sign-in screen since for some reason
    // popUpTo is not working on signOut action.
    requireActivity().finish()
    return false
  }
}
