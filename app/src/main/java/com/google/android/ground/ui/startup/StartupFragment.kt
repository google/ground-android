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
package com.google.android.ground.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.ground.R
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.EphemeralPopups
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class StartupFragment : AbstractFragment() {

  @Inject lateinit var popups: EphemeralPopups

  private lateinit var viewModel: StartupViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(StartupViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? = inflater.inflate(R.layout.startup_frag, container, false)

  override fun onResume() {
    super.onResume()
    showProgressDialog(R.string.initializing)
    viewLifecycleOwner.lifecycleScope.launch {
      try {
        viewModel.initializeLogin()
      } catch (t: Throwable) {
        onInitFailed(t)
      }
    }
  }

  override fun onPause() {
    dismissProgressDialog()
    super.onPause()
  }

  private fun onInitFailed(t: Throwable) {
    Timber.e(t, "Failed to launch app")
    if (t is GooglePlayServicesNotAvailableException) {
      popups.ErrorPopup().show(R.string.google_api_install_failed)
    }
    requireActivity().finish()
  }
}
