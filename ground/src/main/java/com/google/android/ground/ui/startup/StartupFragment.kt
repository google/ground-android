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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.R
import com.google.android.ground.rx.RxAutoDispose
import com.google.android.ground.system.GoogleApiManager
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.EphemeralPopups
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class StartupFragment : AbstractFragment() {

  @Inject lateinit var authenticationManager: AuthenticationManager
  @Inject lateinit var googleApiManager: GoogleApiManager
  @Inject lateinit var popups: EphemeralPopups

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = inflater.inflate(R.layout.startup_frag, container, false)

  override fun onAttach(context: Context) {
    super.onAttach(context)
    googleApiManager
      .installGooglePlayServices()
      .`as`(RxAutoDispose.autoDisposable<Any>(requireActivity()))
      .subscribe({ onGooglePlayServicesReady() }) { t: Throwable -> onGooglePlayServicesFailed(t) }
  }

  private fun onGooglePlayServicesReady() {
    authenticationManager.init()
  }

  private fun onGooglePlayServicesFailed(t: Throwable) {
    Timber.e(t, "Google Play Services install failed")
    popups.showError(R.string.google_api_install_failed)
    requireActivity().finish()
  }
}
