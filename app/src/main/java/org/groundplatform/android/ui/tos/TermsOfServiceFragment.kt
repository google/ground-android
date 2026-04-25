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
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.common.Constants.SURVEY_PATH_SEGMENT
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.surveyselector.SurveySelectorFragmentDirections
import org.groundplatform.android.util.createComposeView
import javax.inject.Inject

@AndroidEntryPoint
class TermsOfServiceFragment : AbstractFragment() {

  @Inject lateinit var popups: EphemeralPopups

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = createComposeView {
    TermsOfServiceScreen(
      isViewOnly = TermsOfServiceFragmentArgs.fromBundle(requireArguments()).isViewOnly,
      onNavigateUp = { findNavController().navigateUp() },
      onNavigateToSurveySelector = {
        activity?.intent?.data?.let { uri ->
          val pathSegments = uri.pathSegments

          if (SURVEY_PATH_SEGMENT in pathSegments) {
            val index = pathSegments.indexOf(SURVEY_PATH_SEGMENT)
            val surveyId = pathSegments.getOrNull(index + 1)
            openSurveySelector(surveyId)
          } else {
            openSurveySelector()
          }
        } ?: run { openSurveySelector() }
      },
      onError = { message -> popups.ErrorPopup().show(message) },
    )
  }

  private fun openSurveySelector(surveyId: String? = null) {
    val action = SurveySelectorFragmentDirections.showSurveySelectorScreen(true)
    action.surveyId = surveyId
    findNavController().navigate(action)
  }
}
