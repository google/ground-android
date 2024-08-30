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

import android.text.Html
import android.text.Spanned
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.google.android.ground.R
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.surveyselector.SurveySelectorFragmentDirections
import javax.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import timber.log.Timber

class TermsOfServiceViewModel
@Inject
constructor(
  private val navigator: Navigator,
  private val termsOfServiceRepository: TermsOfServiceRepository,
  private val popups: EphemeralPopups,
  private val authManager: AuthenticationManager,
) : AbstractViewModel() {
  val agreeCheckboxChecked: MutableLiveData<Boolean> = MutableLiveData()

  val termsOfServiceText: LiveData<Spanned> = liveData {
    try {
      val tos = termsOfServiceRepository.getTermsOfService()?.text ?: ""
      val flavor = CommonMarkFlavourDescriptor()
      val parser = MarkdownParser(flavor)
      val html =
        parser.buildMarkdownTreeFromString(tos).run {
          HtmlGenerator(tos, this, CommonMarkFlavourDescriptor()).generateHtml()
        }
      emit(Html.fromHtml(html, 0))
    } catch (e: Throwable) {
      when (e) {
        is DataStoreException,
        is TimeoutCancellationException -> {
          onGetTosFailure()
        }
        else -> {
          Timber.e("Unexpected error: ${e.message}")
          onGetTosFailure()
        }
      }
    }
  }

  private fun onGetTosFailure() {
    popups.ErrorPopup().show(R.string.load_tos_failed)
    authManager.signOut()
  }

  fun onButtonClicked() {
    termsOfServiceRepository.isTermsOfServiceAccepted = true
    navigator.navigate(SurveySelectorFragmentDirections.showSurveySelectorScreen(true))
  }
}
