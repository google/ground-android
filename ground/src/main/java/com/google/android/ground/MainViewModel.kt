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
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.domain.usecases.survey.ReactivateLastSurveyUseCase
import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.room.LocalDatabase
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.system.auth.SignInState
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.util.isPermissionDeniedException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

  private val _navigationRequests: MutableSharedFlow<MainUiState?> = MutableSharedFlow()
  var navigationRequests: SharedFlow<MainUiState?> = _navigationRequests.asSharedFlow()

  /** The window insets determined by the activity. */
  val windowInsets: MutableLiveData<WindowInsetsCompat> = MutableLiveData()

  init {
    viewModelScope.launch {
      // TODO: Check auth status whenever fragments resumes
      authenticationManager.signInState.collect {
        _navigationRequests.emit(onSignInStateChange(it))
      }
    }
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
      // TODO(#1808): Display some error dialog to the user with a helpful user-readable message.
      onUserSignedOut()
    }
  }

  private fun Throwable.isSignInCancelledException() =
    this is ApiException && statusCode == SIGN_IN_CANCELLED

  private fun onUserSignedOut(): MainUiState {
    // Scope of subscription is until view model is cleared. Dispose it manually otherwise, firebase
    // attempts to maintain a connection even after user has logged out and throws an error.
    surveyRepository.clearActiveSurvey()
    userRepository.clearUserPreferences()

    // TODO(#1691): Once multi-user login is supported, avoid clearing local db data. This is
    //  currently being done to prevent one user's data to be submitted as another user after
    //  re-login.
    viewModelScope.launch { withContext(ioDispatcher) { localDatabase.clearAllTables() } }
    return MainUiState.OnUserSignedOut
  }

  private suspend fun onUserSignedIn(user: User): MainUiState =
    try {
      userRepository.saveUserDetails(user)
      if (!isTosAccepted()) {
        MainUiState.TosNotAccepted
      } else if (!attemptToReactiveLastActiveSurvey()) {
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

  /** Returns true if the last survey was successfully reactivated, if any. */
  private suspend fun attemptToReactiveLastActiveSurvey(): Boolean {
    reactivateLastSurvey()
    return surveyRepository.selectedSurveyId != null
  }
}
