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
package com.google.android.ground

import android.content.SharedPreferences
import android.os.Looper
import androidx.navigation.NavDirections
import com.google.android.ground.persistence.local.room.LocalDataStoreException
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.SignInState.Companion.error
import com.google.android.ground.system.auth.SignInState.Companion.signingIn
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.signin.SignInFragmentDirections
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sharedtest.FakeData
import com.sharedtest.TestObservers.observeUntilFirstChange
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.observers.TestObserver
import javax.inject.Inject
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest : BaseHiltTest() {

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var viewModel: MainViewModel
  @Inject lateinit var navigator: Navigator
  @Inject lateinit var sharedPreferences: SharedPreferences
  @Inject lateinit var tosRepository: TermsOfServiceRepository
  @Inject lateinit var userRepository: UserRepository

  private lateinit var navDirectionsTestObserver: TestObserver<NavDirections>

  @Before
  override fun setUp() {
    super.setUp()

    fakeAuthenticationManager.setUser(FakeData.USER)

    // Subscribe to navigation requests
    navDirectionsTestObserver = navigator.getNavigateRequests().test()
  }

  private fun setupUserPreferences() {
    sharedPreferences.edit().putString("foo", "bar").apply()
  }

  private fun verifyUserPreferencesCleared() {
    assertThat(sharedPreferences.all).isEmpty()
  }

  private fun verifyUserSaved() = runWithTestDispatcher {
    assertThat(userRepository.getUser(FakeData.USER.id)).isEqualTo(FakeData.USER)
  }

  private fun verifyUserNotSaved() = runWithTestDispatcher {
    assertFailsWith<LocalDataStoreException> { userRepository.getUser(FakeData.USER.id) }
  }

  private fun verifyProgressDialogVisible(visible: Boolean) {
    observeUntilFirstChange(viewModel.signInProgressDialogVisibility)
    assertThat(viewModel.signInProgressDialogVisibility.value).isEqualTo(visible)
  }

  private fun verifyNavigationRequested(vararg navDirections: NavDirections) {
    navDirectionsTestObserver.assertNoErrors()
    navDirectionsTestObserver.assertNotComplete()
    navDirectionsTestObserver.assertValues(*navDirections)
  }

  @Test
  fun testSignInStateChanged_onSignedOut() {
    setupUserPreferences()
    fakeAuthenticationManager.signOut()
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verifyProgressDialogVisible(false)
    verifyNavigationRequested(SignInFragmentDirections.showSignInScreen())
    verifyUserPreferencesCleared()
    verifyUserNotSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  @Test
  fun testSignInStateChanged_onSigningIn() {
    fakeAuthenticationManager.setState(signingIn())
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verifyProgressDialogVisible(true)
    verifyNavigationRequested()
    verifyUserNotSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  // TODO(#1612): Add back testSignInStateChanged_onSignedIn_whenTosAcceptedAndActiveSurveyAvailable
  //   once reactivate last survey is implemented.

  @Test
  fun testSignInStateChanged_onSignedIn_whenTosNotAccepted() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.termsOfService = Result.success(FakeData.TERMS_OF_SERVICE)
    fakeAuthenticationManager.signIn()
    advanceUntilIdle()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    verifyProgressDialogVisible(false)
    verifyNavigationRequested(
      SignInFragmentDirections.showTermsOfService()
        .setTermsOfServiceText(FakeData.TERMS_OF_SERVICE.text) as NavDirections
    )
    verifyUserSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  @Test
  fun testSignInStateChanged_onSignedIn_getTos_whenTosMissing() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.termsOfService = null
    fakeAuthenticationManager.signIn()
    advanceUntilIdle()
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verifyProgressDialogVisible(false)
    verifyNavigationRequested(SignInFragmentDirections.showSurveySelectorScreen(true))
    verifyUserSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  @Test
  fun testSignInStateChanged_onSignedIn_getTos_whenPermissionDenied() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.termsOfService =
      Result.failure(
        FirebaseFirestoreException(
          "permission denied",
          FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
      )
    fakeAuthenticationManager.signIn()
    advanceUntilIdle()
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verifyProgressDialogVisible(false)
    verifyNavigationRequested(SignInFragmentDirections.showPermissionDeniedDialogFragment())
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  @Test
  fun testSignInStateChanged_onSignedIn_getTos_whenNotPermissionDeniedError() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = false
      fakeRemoteDataStore.termsOfService = Result.failure(Error("user error"))
      fakeAuthenticationManager.signIn()
      advanceUntilIdle()
      Shadows.shadowOf(Looper.getMainLooper()).idle()

      verifyProgressDialogVisible(false)
      verifyNavigationRequested(SignInFragmentDirections.showSignInScreen())
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    }

  @Test
  fun testSignInStateChanged_onSignInError() {
    setupUserPreferences()

    fakeAuthenticationManager.setState(error(Exception()))
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verifyProgressDialogVisible(false)
    verifyNavigationRequested(SignInFragmentDirections.showSignInScreen())
    verifyUserPreferencesCleared()
    verifyUserNotSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }
}
