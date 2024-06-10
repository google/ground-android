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
import androidx.navigation.NavDirections
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.domain.usecases.survey.ReactivateLastSurveyUseCase
import com.google.android.ground.persistence.local.room.LocalDatabase
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.system.auth.SignInState
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.signin.SignInFragmentDirections
import com.google.android.ground.ui.surveyselector.SurveySelectorFragmentDirections
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
  navigator: Navigator,
  authenticationManager: AuthenticationManager,
) : AbstractViewModel() {

  private val _uiState: MutableStateFlow<MainUiState?> = MutableStateFlow(null)
  var uiState: StateFlow<MainUiState?> = _uiState.asStateFlow()

  /** The window insets determined by the activity. */
  val windowInsets: MutableLiveData<WindowInsetsCompat> = MutableLiveData()

  /** The state of sign in progress dialog visibility. */
  val signInProgressDialogVisibility: MutableLiveData<Boolean> = MutableLiveData()

  init {
    viewModelScope.launch {
      // TODO: Check auth status whenever fragments resumes
      authenticationManager.signInState.collect {
        val nextState = onSignInStateChange(it)
        nextState?.let { navigator.navigate(nextState) }
      }
    }
  }

  private suspend fun onSignInStateChange(signInState: SignInState): NavDirections? {
    // Display progress only when signing in.
    signInProgressDialogVisibility.postValue(signInState.state == SignInState.State.SIGNING_IN)

    return signInState.result.fold(
      {
        when (signInState.state) {
          SignInState.State.SIGNED_IN -> onUserSignedIn()
          SignInState.State.SIGNED_OUT -> onUserSignedOut()
          else -> null
        }
      },
      { onUserSignInError(it) },
    )
  }

  private suspend fun onUserSignInError(error: Throwable): NavDirections? {
    Timber.e(error, "Sign in failed")
    return if (error.isPermissionDeniedException()) {
      _uiState.emit(MainUiState.onPermissionDenied)
      null
    } else {
      // TODO(#1808): Display some error dialog to the user with a helpful user-readable messagez.
      onUserSignedOut()
    }
  }

  private fun onUserSignedOut(): NavDirections {
    // Scope of subscription is until view model is cleared. Dispose it manually otherwise, firebase
    // attempts to maintain a connection even after user has logged out and throws an error.
    surveyRepository.clearActiveSurvey()
    userRepository.clearUserPreferences()

    // TODO(#1691): Once multi-user login is supported, avoid clearing local db data. This is
    //  currently being done to prevent one user's data to be submitted as another user after
    //  re-login.
    viewModelScope.launch { withContext(ioDispatcher) { localDatabase.clearAllTables() } }

    return SignInFragmentDirections.showSignInScreen()
  }

  private suspend fun onUserSignedIn(): NavDirections? =
    try {
      userRepository.saveUserDetails()
      val tos = termsOfServiceRepository.getTermsOfService()
      if (tos == null || termsOfServiceRepository.isTermsOfServiceAccepted) {
        reactivateLastSurvey()
        getDirectionAfterSignIn()
      } else {
        SignInFragmentDirections.showTermsOfService(false)
      }
    } catch (e: Throwable) {
      onUserSignInError(e)
    }

  private fun getDirectionAfterSignIn(): NavDirections =
    if (surveyRepository.selectedSurveyId != null) {
      HomeScreenFragmentDirections.showHomeScreen()
    } else {
      SurveySelectorFragmentDirections.showSurveySelectorScreen(true)
    }
}
