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
import androidx.navigation.NavDirections
import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.system.auth.SignInState
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.signin.SignInFragmentDirections
import com.google.android.ground.ui.surveyselector.SurveySelectorFragmentDirections
import io.reactivex.Maybe
import io.reactivex.Observable
import javax.inject.Inject
import timber.log.Timber

/** Top-level view model representing state of the [MainActivity] shared by all fragments. */
@SharedViewModel
class MainViewModel
@Inject
constructor(
  private val localValueStore: LocalValueStore,
  private val surveyRepository: SurveyRepository,
  private val userRepository: UserRepository,
  private val termsOfServiceRepository: TermsOfServiceRepository,
  private val popups: EphemeralPopups,
  navigator: Navigator,
  authenticationManager: AuthenticationManager,
  schedulers: Schedulers
) : AbstractViewModel() {

  /** The window insets determined by the activity. */
  val windowInsets: MutableLiveData<WindowInsetsCompat> = MutableLiveData()

  /** The state of sign in progress dialog visibility. */
  val signInProgressDialogVisibility: MutableLiveData<Boolean> = MutableLiveData()

  init {
    disposeOnClear(
      authenticationManager.signInState
        .observeOn(schedulers.ui())
        .switchMap { signInState: SignInState -> onSignInStateChange(signInState) }
        .subscribe { directions: NavDirections -> navigator.navigate(directions) }
    )
  }

  private fun onSignInStateChange(signInState: SignInState): Observable<NavDirections> {
    // Display progress only when signing in.
    signInProgressDialogVisibility.postValue(signInState.state == SignInState.State.SIGNING_IN)

    // TODO: Check auth status whenever fragments resumes
    return signInState.result.fold(
      {
        when (signInState.state) {
          SignInState.State.SIGNED_IN -> onUserSignedIn(it!!)
          SignInState.State.SIGNED_OUT -> onUserSignedOut()
          else -> Observable.never()
        }
      },
      { onUserSignInError(it) }
    )
  }

  private fun onUserSignInError(error: Throwable): Observable<NavDirections> {
    Timber.e("Authentication error: $error")
    popups.showError(R.string.sign_in_unsuccessful)
    return onUserSignedOut()
  }

  private fun onUserSignedOut(): Observable<NavDirections> {
    // Scope of subscription is until view model is cleared. Dispose it manually otherwise, firebase
    // attempts to maintain a connection even after user has logged out and throws an error.
    surveyRepository.clearActiveSurvey()
    userRepository.clearUserPreferences()
    return Observable.just(SignInFragmentDirections.showSignInScreen())
  }

  private fun onUserSignedIn(user: User): Observable<NavDirections> =
    userRepository
      .saveUser(user)
      .andThen(
        if (termsOfServiceRepository.isTermsOfServiceAccepted) {
          Observable.just(getDirectionAfterSignIn())
        } else {
          termsOfServiceRepository.termsOfService
            .map { SignInFragmentDirections.showTermsOfService().setTermsOfServiceText(it.text) }
            .cast(NavDirections::class.java)
            .switchIfEmpty(Maybe.just(getDirectionAfterSignIn()))
            .toObservable()
        }
      )

  private fun getDirectionAfterSignIn(): NavDirections =
    if (localValueStore.lastActiveSurveyId != "") {
      HomeScreenFragmentDirections.showHomeScreen()
    } else {
      SurveySelectorFragmentDirections.showSurveySelectorScreen(true)
    }
}
