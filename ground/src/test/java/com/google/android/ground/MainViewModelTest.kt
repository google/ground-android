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
import app.cash.turbine.test
import com.google.android.ground.persistence.local.room.LocalDataStoreException
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.SignInState
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest : BaseHiltTest() {

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var viewModel: MainViewModel
  @Inject lateinit var sharedPreferences: SharedPreferences
  @Inject lateinit var tosRepository: TermsOfServiceRepository
  @Inject lateinit var userRepository: UserRepository

  @Before
  override fun setUp() {
    super.setUp()

    fakeAuthenticationManager.setUser(FakeData.USER)
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

  private fun verifyUiState(uiState: MainUiState) = runWithTestDispatcher {
    viewModel.getUiState().test { assertThat(expectMostRecentItem()).isEqualTo(uiState) }
  }

  @Test
  fun testSignInStateChanged_onSignedOut() = runWithTestDispatcher {
    setupUserPreferences()

    fakeAuthenticationManager.signOut()
    advanceUntilIdle()

    verifyUiState(MainUiState.OnUserSignedOut)
    verifyUserPreferencesCleared()
    verifyUserNotSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  @Test
  fun testSignInStateChanged_onSigningIn() = runWithTestDispatcher {
    fakeAuthenticationManager.setState(SignInState.SigningIn)
    advanceUntilIdle()

    verifyUiState(MainUiState.OnUserSigningIn)
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

    verifyUiState(MainUiState.TosNotAccepted)
    verifyUserSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }

  @Test
  fun testSignInStateChanged_onSignedIn_getTos_whenTosMissing() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.termsOfService = null

    fakeAuthenticationManager.signIn()
    advanceUntilIdle()

    verifyUiState(MainUiState.NoActiveSurvey)
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
          FirebaseFirestoreException.Code.PERMISSION_DENIED,
        )
      )

    fakeAuthenticationManager.signIn()
    advanceUntilIdle()

    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    verifyUiState(MainUiState.OnPermissionDenied)
  }

  @Test
  fun testSignInStateChanged_onSignedIn_getTos_whenNotPermissionDeniedError() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = false
      fakeRemoteDataStore.termsOfService = Result.failure(Error("user error"))

      fakeAuthenticationManager.signIn()
      advanceUntilIdle()

      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
      verifyUiState(MainUiState.OnUserSignedOut)
    }

  @Test
  fun testSignInStateChanged_onSignInError() = runWithTestDispatcher {
    setupUserPreferences()

    fakeAuthenticationManager.setState(SignInState.Error(Exception()))
    advanceUntilIdle()

    verifyUiState(MainUiState.OnUserSignedOut)
    verifyUserPreferencesCleared()
    verifyUserNotSaved()
    assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
  }
}
