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
package org.groundplatform.android

import android.net.Uri
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED
import com.google.android.gms.common.api.ApiException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.groundplatform.android.Config.SURVEY_PATH_SEGMENT
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.model.User
import org.groundplatform.android.persistence.local.room.LocalDatabase
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.TermsOfServiceRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.android.system.auth.SignInState
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.common.SharedViewModel
import org.groundplatform.android.usecases.survey.ReactivateLastSurveyUseCase
import org.groundplatform.android.util.isPermissionDeniedException
import timber.log.Timber

/** Top-level view model representing state of the [MainActivity] shared by all fragments. */
@SharedViewModel
class MainViewModel
@Inject
constructor(
  private val localDatabase: LocalDatabase,
  private val surveyRepository: SurveyRepository,
  private val userRepository: UserRepository,
  private val termsOfServiceRepository: TermsOfServiceRepository,
  private val reactivateLastSurvey: ReactivateLastSurveyUseCase,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  authenticationManager: AuthenticationManager,
) : AbstractViewModel() {

  private val _navigationRequests: MutableSharedFlow<MainUiState?> = MutableSharedFlow()
  var navigationRequests: SharedFlow<MainUiState?> = _navigationRequests.asSharedFlow()

  /** The window insets determined by the activity. */
  val windowInsets: MutableLiveData<WindowInsetsCompat> = MutableLiveData()

  private val _deepLinkUri = MutableStateFlow<Uri?>(null)

  init {
    viewModelScope.launch {
      // TODO: Check auth status whenever fragments resumes
      // Issue URL: https://github.com/google/ground-android/issues/2624
      authenticationManager.signInState.collect {
        _navigationRequests.emit(onSignInStateChange(it))
      }
    }
  }

  private fun isDeepLinkAvailable(): Boolean = _deepLinkUri.value != null

  fun setDeepLinkUri(uri: Uri) {
    _deepLinkUri.value = uri
  }

  private suspend fun onSignInStateChange(signInState: SignInState): MainUiState =
    when (signInState) {
      is SignInState.Error -> onUserSignInError(signInState.error)
      is SignInState.SignedIn -> onUserSignedIn(signInState.user)
      is SignInState.SignedOut -> onUserSignedOut()
      is SignInState.SigningIn -> MainUiState.OnUserSigningIn
    }

  private fun onUserSignInError(error: Throwable): MainUiState {
    Timber.e(error, "Sign in failed")
    return if (error.isPermissionDeniedException()) {
      MainUiState.OnPermissionDenied
    } else if (error.isSignInCancelledException()) {
      Timber.d("User cancelled sign in")
      MainUiState.OnUserSignedOut
    } else {
      // TODO: Display some error dialog to the user with a helpful user-readable message.
      // Issue URL: https://github.com/google/ground-android/issues/1808
      onUserSignedOut()
    }
  }

  private fun Throwable.isSignInCancelledException() =
    this is ApiException && statusCode == SIGN_IN_CANCELLED

  private fun onUserSignedOut(): MainUiState {
    // Scope of subscription is until view model is cleared. Dispose it manually otherwise, firebase
    // attempts to maintain a connection even after user has logged out and throws an error.

    // TODO: Once multi-user login is supported, avoid clearing local db data. This is
    //  currently being done to prevent one user's data to be submitted as another user after
    //  re-login.
    // Issue URL: https://github.com/google/ground-android/issues/1691
    viewModelScope.launch {
      withContext(ioDispatcher) {
        surveyRepository.clearActiveSurvey()
        userRepository.clearUserPreferences()
        localDatabase.clearAllTables()
      }
    }
    return MainUiState.OnUserSignedOut
  }

  private suspend fun onUserSignedIn(user: User): MainUiState =
    try {
      userRepository.saveUserDetails(user)
      if (!isTosAccepted()) {
        MainUiState.TosNotAccepted
      } else if (isDeepLinkAvailable()) {
        val deepLinkUri = _deepLinkUri.value
        val pathSegments = deepLinkUri?.pathSegments ?: emptyList()

        val surveyId =
          pathSegments
            .indexOf(SURVEY_PATH_SEGMENT)
            .takeIf { it != -1 }
            ?.let { pathSegments.getOrNull(it + 1) }

        if (!surveyId.isNullOrBlank()) {
          MainUiState.ActiveSurveyById(surveyId)
        } else {
          MainUiState.NoActiveSurvey
        }
      } else if (!reactivateLastSurvey()) {
        MainUiState.NoActiveSurvey
      } else {
        // Everything is fine, show the home screen
        MainUiState.ShowHomeScreen
      }
    } catch (e: Throwable) {
      onUserSignInError(e)
    }

  /** Returns true if the user has already accepted the Terms of Service. */
  private fun isTosAccepted(): Boolean = termsOfServiceRepository.isTermsOfServiceAccepted
}
