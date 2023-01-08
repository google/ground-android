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
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.system.auth.SignInState
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.signin.SignInFragmentDirections
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java8.util.Optional
import javax.inject.Inject
import timber.log.Timber

/** Top-level view model representing state of the [MainActivity] shared by all fragments. */
@SharedViewModel
class MainViewModel
@Inject
constructor(
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val userRepository: UserRepository,
  private val termsOfServiceRepository: TermsOfServiceRepository,
  private val popups: EphemeralPopups,
  navigator: Navigator,
  authenticationManager: AuthenticationManager,
  private val schedulers: Schedulers
) : AbstractViewModel() {

  private var surveySyncSubscription: Disposable? = null

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

  /**
   * Keeps local locations o interest in sync with remote when a survey is active, does nothing when
   * no survey is active. The stream never completes; syncing stops when subscriptions are disposed
   * of.
   *
   * @param survey the currently active survey.
   */
  private fun syncLocationsOfInterest(
    survey: Optional<Survey>
  ): @Cold(terminates = false) Completable {
    return survey
      .map { locationOfInterestRepository.syncLocationsOfInterest(it) }
      .orElse(Completable.never())
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
    surveySyncSubscription?.dispose()

    surveyRepository.clearActiveSurvey()
    userRepository.clearUserPreferences()
    return Observable.just(SignInFragmentDirections.showSignInScreen())
  }

  private fun onUserSignedIn(user: User): Observable<NavDirections> {
    // TODO: Move to background service.
    surveySyncSubscription =
      surveyRepository.activeSurvey
        .observeOn(schedulers.io())
        .switchMapCompletable { syncLocationsOfInterest(it) }
        .subscribe()
    surveySyncSubscription?.let { disposeOnClear(it) }

    return userRepository
      .saveUser(user)
      .andThen(
        if (termsOfServiceRepository.isTermsOfServiceAccepted) {
          Observable.just(HomeScreenFragmentDirections.showHomeScreen())
        } else {
          termsOfServiceRepository.termsOfService
            .map { SignInFragmentDirections.showTermsOfService().setTermsOfServiceText(it.text) }
            .cast(NavDirections::class.java)
            .switchIfEmpty(Maybe.just(HomeScreenFragmentDirections.showHomeScreen()))
            .toObservable()
        }
      )
  }
}
