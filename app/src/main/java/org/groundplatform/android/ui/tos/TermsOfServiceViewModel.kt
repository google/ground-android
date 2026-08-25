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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.android.system.deeplink.PlayInstallReferrerService
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.util.isPermissionDeniedException
import org.groundplatform.domain.repository.TermsOfServiceRepositoryInterface
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import timber.log.Timber

sealed interface TosUiState {
  val isViewOnly: Boolean

  data class Loading(override val isViewOnly: Boolean) : TosUiState

  data class Success(
    val tosHtml: String,
    val agreeChecked: Boolean,
    override val isViewOnly: Boolean,
  ) : TosUiState

  data class Error(override val isViewOnly: Boolean) : TosUiState
}

sealed interface TosEvent {
  data class NavigateToSurveySelector(val deferredSurveyId: String?) : TosEvent

  object LoadError : TosEvent
}

@HiltViewModel
class TermsOfServiceViewModel
@Inject
constructor(
  private val authManager: AuthenticationManager,
  private val termsOfServiceRepository: TermsOfServiceRepositoryInterface,
  private val playInstallReferrerService: PlayInstallReferrerService,
  savedStateHandle: SavedStateHandle,
) : AbstractViewModel() {

  private val isViewOnly: Boolean = savedStateHandle["isViewOnly"] ?: false

  private val _uiState = MutableStateFlow<TosUiState>(TosUiState.Loading(isViewOnly))
  val uiState: StateFlow<TosUiState> = _uiState.asStateFlow()

  private val _events = Channel<TosEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  init {
    loadTermsOfService()
  }

  fun setAgreeCheckboxChecked(checked: Boolean) {
    _uiState.update { state ->
      if (state is TosUiState.Success) {
        state.copy(agreeChecked = checked)
      } else {
        state
      }
    }
  }

  fun onAgreeButtonClicked() {
    viewModelScope.launch {
      termsOfServiceRepository.isTermsOfServiceAccepted = true
      val deferredSurveyId = playInstallReferrerService.getDeferredSurveyId()
      _events.send(TosEvent.NavigateToSurveySelector(deferredSurveyId))
    }
  }

  fun onRetryClicked() {
    loadTermsOfService()
  }

  private fun loadTermsOfService() {
    viewModelScope.launch {
      _uiState.value = TosUiState.Loading(isViewOnly)
      try {
        val tos = termsOfServiceRepository.getTermsOfService()?.text ?: ""
        val flavor = CommonMarkFlavourDescriptor()
        val parser = MarkdownParser(flavor)
        val html =
          parser.buildMarkdownTreeFromString(text = tos).run {
            HtmlGenerator(tos, this, flavor).generateHtml()
          }
        _uiState.value = TosUiState.Success(html, agreeChecked = false, isViewOnly)
      } catch (e: Throwable) {
        if (!e.isExpectedFailure()) {
          Timber.e(e, "Failed to load Terms of Service")
        }
        if (isViewOnly) {
          _uiState.value = TosUiState.Error(isViewOnly)
        } else {
          _events.send(TosEvent.LoadError)
          authManager.signOut()
        }
      }
    }
  }

  private fun Throwable.isExpectedFailure() =
    this is TimeoutCancellationException || isPermissionDeniedException()
}
