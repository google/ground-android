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
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.SignInState.Companion.error
import com.google.android.ground.system.auth.SignInState.Companion.signingIn
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.signin.SignInFragmentDirections
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.TestObservers.observeUntilFirstChange
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.observers.TestObserver
import java8.util.Optional
import javax.inject.Inject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast

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
    // TODO: Add a test for syncLois
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

  private fun verifyUserSaved() {
    userRepository.getUser(FakeData.USER.id).test().assertResult(FakeData.USER)
  }

  private fun verifyUserNotSaved() {
    userRepository.getUser(FakeData.USER.id).test().assertError(NoSuchElementException::class.java)
  }

  private fun verifyProgressDialogVisible(visible: Boolean) {
    observeUntilFirstChange(viewModel.signInProgressDialogVisibility)
    assertThat(viewModel.signInProgressDialogVisibility.value).isEqualTo(visible)
  }

  private fun verifyNavigationRequested(vararg navDirections: NavDirections) {
    navDirectionsTestObserver.assertNoErrors().assertNotComplete().assertValues(*navDirections)
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

  @Test
  fun testSignInStateChanged_onSignedIn_whenTosAccepted() {
    tosRepository.isTermsOfServiceAccepted = true
    fakeRemoteDataStore.setTermsOfService(Optional.of(FakeData.TERMS_OF_SERVICE))
    fakeAuthenticationManager.signIn()
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verifyProgressDialogVisible(false)
    verifyNavigationRequested(HomeScreenFragmentDirections.showHomeScreen())
    verifyUserSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun testSignInStateChanged_onSignedIn_whenTosNotAccepted() {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.setTermsOfService(Optional.of(FakeData.TERMS_OF_SERVICE))
    fakeAuthenticationManager.signIn()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    verifyProgressDialogVisible(false)
    verifyNavigationRequested(
      (SignInFragmentDirections.showTermsOfService()
        .setTermsOfServiceText(FakeData.TERMS_OF_SERVICE.text) as NavDirections)
    )
    verifyUserSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  @Test
  fun testSignInStateChanged_onSignedIn_whenTosMissing() {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.setTermsOfService(Optional.empty())
    fakeAuthenticationManager.signIn()
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verifyProgressDialogVisible(false)
    verifyNavigationRequested(HomeScreenFragmentDirections.showHomeScreen())
    verifyUserSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  @Test
  fun testSignInStateChanged_onSignInError() {
    setupUserPreferences()

    fakeAuthenticationManager.setState(error(Exception()))
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Sign in unsuccessful")
    verifyProgressDialogVisible(false)
    verifyNavigationRequested(SignInFragmentDirections.showSignInScreen())
    verifyUserPreferencesCleared()
    verifyUserNotSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }
}
