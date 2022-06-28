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
package com.google.android.gnd

import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.google.android.gnd.model.Survey
import com.google.android.gnd.repository.FeatureRepository
import com.google.android.gnd.repository.SurveyRepository
import com.google.android.gnd.repository.TermsOfServiceRepository
import com.google.android.gnd.repository.UserRepository
import com.google.android.gnd.rx.RxTransformers.switchMapIfPresent
import com.google.android.gnd.rx.Schedulers
import com.google.android.gnd.rx.annotations.Cold
import com.google.android.gnd.system.auth.AuthenticationManager
import com.google.android.gnd.system.auth.SignInState
import com.google.android.gnd.ui.common.AbstractViewModel
import com.google.android.gnd.ui.common.EphemeralPopups
import com.google.android.gnd.ui.common.Navigator
import com.google.android.gnd.ui.common.SharedViewModel
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections
import com.google.android.gnd.ui.signin.SignInFragmentDirections
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import java8.util.Optional
import timber.log.Timber
import javax.inject.Inject

/** Top-level view model representing state of the [MainActivity] shared by all fragments.  */
@SharedViewModel
class MainViewModel @Inject constructor(
    private val surveyRepository: SurveyRepository,
    private val featureRepository: FeatureRepository,
    private val userRepository: UserRepository,
    private val termsOfServiceRepository: TermsOfServiceRepository,
    private val popups: EphemeralPopups,
    navigator: Navigator,
    authenticationManager: AuthenticationManager,
    schedulers: Schedulers
) : AbstractViewModel() {

    /** The window insets determined by the activity.  */
    val windowInsets: MutableLiveData<WindowInsetsCompat> = MutableLiveData()

    /** The state of sign in progress dialog visibility.  */
    val signInProgressDialogVisibility: MutableLiveData<Boolean> = MutableLiveData()

    init {
        // TODO: Move to background service.
        disposeOnClear(
            surveyRepository
                .activeSurvey
                .observeOn(schedulers.io())
                .switchMapCompletable { syncFeatures(it) }
                .subscribe()
        )

        disposeOnClear(
            authenticationManager
                .signInState
                .compose(switchMapIfPresent(SignInState::user) { userRepository.saveUser(it) })
                .observeOn(schedulers.ui())
                .switchMap { signInState: SignInState -> onSignInStateChange(signInState) }
                .subscribe { directions: NavDirections -> navigator.navigate(directions) })
    }

    /**
     * Keeps local features in sync with remote when a survey is active, does nothing when no survey
     * is active. The stream never completes; syncing stops when subscriptions are disposed of.
     *
     * @param survey the currently active survey.
     */
    private fun syncFeatures(survey: Optional<Survey>): @Cold(terminates = false) Completable {
        return survey.map { featureRepository.syncFeatures(it) }
            .orElse(Completable.never())
    }

    private fun onSignInStateChange(signInState: SignInState): Observable<NavDirections> {
        // Display progress only when signing in.
        signInProgressDialogVisibility.postValue(signInState.state == SignInState.State.SIGNING_IN)

        // TODO: Check auth status whenever fragments resumes
        return when (signInState.state) {
            SignInState.State.SIGNED_IN -> onUserSignedIn()
            SignInState.State.SIGNED_OUT -> onUserSignedOut()
            SignInState.State.ERROR -> onUserSignInError(signInState.error())
            else -> Observable.never()
        }
    }

    private fun onUserSignInError(error: Optional<Throwable?>): Observable<NavDirections> {
        Timber.e("Authentication error: $error")
        popups.showError(R.string.sign_in_unsuccessful)
        return onUserSignedOut()
    }

    private fun onUserSignedOut(): Observable<NavDirections> {
        surveyRepository.clearActiveSurvey()
        userRepository.clearUserPreferences()
        return Observable.just(SignInFragmentDirections.showSignInScreen())
    }

    private fun onUserSignedIn(): Observable<NavDirections> {
        return if (termsOfServiceRepository.isTermsOfServiceAccepted) {
            Observable.just(HomeScreenFragmentDirections.showHomeScreen())
        } else {
            termsOfServiceRepository
                .termsOfService
                .map {
                    SignInFragmentDirections.showTermsOfService().setTermsOfServiceText(it.text)
                }
                .cast(NavDirections::class.java)
                .switchIfEmpty(Maybe.just(HomeScreenFragmentDirections.showHomeScreen()))
                .toObservable()
        }
    }
}
