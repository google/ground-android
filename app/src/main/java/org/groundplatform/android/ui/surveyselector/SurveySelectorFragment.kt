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
package org.groundplatform.android.ui.surveyselector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.home.HomeScreenFragmentDirections
import org.groundplatform.android.ui.theme.AppTheme

/** User interface implementation of survey selector screen. */
@AndroidEntryPoint
class SurveySelectorFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var ephemeralPopups: EphemeralPopups

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View =
    ComposeView(requireContext()).apply {
      setContent {
        AppTheme {
          SurveySelectorScreen(
            onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
            onNavigateToHomeScreen = {
              findNavController().navigate(HomeScreenFragmentDirections.showHomeScreen())
            },
            onError = { error ->
              if (error is kotlinx.coroutines.TimeoutCancellationException) {
                ephemeralPopups.ErrorPopup().show(R.string.survey_load_timeout_error)
              } else {
                ephemeralPopups.ErrorPopup().unknownError()
              }
            },
          )
        }
      }
    }

  /** Returns true if the app should finish when the back button is pressed. */
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
