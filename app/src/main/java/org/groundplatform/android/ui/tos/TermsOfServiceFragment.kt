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

import android.R
import android.os.Bundle
import android.text.Html.fromHtml
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.common.Constants.SURVEY_PATH_SEGMENT
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.surveyselector.SurveySelectorFragmentDirections
import org.groundplatform.android.util.createComposeView

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
      termsContent = { html -> TermsTextView(fromHtml(html, 0)) },
    )
  }

  private fun openSurveySelector(surveyId: String? = null) {
    val action = SurveySelectorFragmentDirections.showSurveySelectorScreen(true)
    action.surveyId = surveyId
    findNavController().navigate(action)
  }

  @Composable
  private fun TermsTextView(termsText: Spanned) {
    AndroidView(
      factory = { context ->
        TextView(context).apply {
          movementMethod = LinkMovementMethod.getInstance()
          autoLinkMask = Linkify.WEB_URLS
          setTextAppearance(R.style.TextAppearance_Material_Body1)
        }
      },
      update = { textView ->
        textView.text = termsText
        Linkify.addLinks(textView, Linkify.WEB_URLS)
      },
      modifier = Modifier.padding(8.dp),
    )
  }
}
