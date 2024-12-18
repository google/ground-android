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
package com.google.android.ground

import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED
import com.google.android.gms.common.api.ApiException
import com.google.android.ground.MainUiState.*
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.domain.usecases.survey.ReactivateLastSurveyUseCase
import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.room.LocalDatabase
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.system.auth.SignInState
import com.google.android.ground.system.auth.SignInState.*
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.util.isPermissionDeniedException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

  private val _mainUiState = MutableStateFlow<MainUiState?>(null)
  var mainUiState: StateFlow<MainUiState?> = _mainUiState.asStateFlow()

  /** The window insets determined by the activity. */
  val windowInsets: MutableLiveData<WindowInsetsCompat> = MutableLiveData()

  init {
    viewModelScope.launch { authenticationManager.signInState.collect { onSignInStateChange(it) } }
  }

  /** Reacts to changes to the authentication state, updating the main UI state accordingly. */
  private suspend fun onSignInStateChange(signInState: SignInState) {
    Timber.d("Sign in state changed. New state: $signInState")
    when (signInState) {
      is SigningIn -> _mainUiState.value = SignInProgressDialog
      is SignedIn -> onUserSignedIn(signInState.user)
      is SignedOut -> onUserSignedOut()
      is Error -> onUserSignInError(signInState.error)
    }
  }

  private fun onUserSignInError(error: Throwable) {
    if (error.isPermissionDeniedException()) {
      Timber.d(error, "User does not have permission to sign in")
      _mainUiState.value = PermissionDeniedDialog
    } else if (error.isSignInCancelledException()) {
      Timber.d("User cancelled sign in")
      _mainUiState.value = SignInScreen
    } else {
      Timber.d(error, "Unknown sign in error")
      // TODO(#1808): Display some error dialog to the user with a helpful user-readable message.
      onUserSignedOut()
    }
  }

  private fun Throwable.isSignInCancelledException() =
    this is ApiException && statusCode == SIGN_IN_CANCELLED

  private fun onUserSignedOut() {
    // Scope of subscription is until view model is cleared. Dispose it manually otherwise, firebase
    // attempts to maintain a connection even after user has logged out and throws an error.
    surveyRepository.clearActiveSurvey()
    userRepository.clearUserPreferences()

    // TODO(#1691): Once multi-user login is supported, avoid clearing local db data. This is
    //  currently being done to prevent one user's data to be submitted as another user after
    //  re-login.
    viewModelScope.launch { withContext(ioDispatcher) { localDatabase.clearAllTables() } }
    _mainUiState.value = SignInScreen
  }

  private suspend fun onUserSignedIn(user: User) =
    try {
      userRepository.saveUserDetails(user)
      if (!isTosAccepted()) {
        _mainUiState.value = TermsOfService
      } else if (!attemptToReactiveLastActiveSurvey()) {
        _mainUiState.value = SurveySelector
      } else {
        // Everything is fine, show the home screen
        _mainUiState.value = HomeScreen
      }
    } catch (e: Throwable) {
      onUserSignInError(e)
    }

  /** Returns true if the user has already accepted the Terms of Service. */
  private fun isTosAccepted(): Boolean = termsOfServiceRepository.isTermsOfServiceAccepted

  /** Returns true if the last survey was successfully reactivated, if any. */
  private suspend fun attemptToReactiveLastActiveSurvey(): Boolean {
    reactivateLastSurvey()
    return surveyRepository.selectedSurveyId != null
  }
}
