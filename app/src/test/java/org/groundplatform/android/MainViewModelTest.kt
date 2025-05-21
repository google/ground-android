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
package org.groundplatform.android

import android.content.SharedPreferences
import androidx.core.content.edit
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.persistence.local.room.LocalDataStoreException
import org.groundplatform.android.persistence.remote.FakeRemoteDataStore
import org.groundplatform.android.repository.TermsOfServiceRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.system.auth.SignInState
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
    sharedPreferences.edit { putString("foo", "bar") }
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

  @Test
  fun testSignInStateChanged_onSignedOut() = runWithTestDispatcher {
    setupUserPreferences()

    viewModel.navigationRequests.test {
      fakeAuthenticationManager.signOut()
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(MainUiState.OnUserSignedOut)
      verifyUserPreferencesCleared()
      verifyUserNotSaved()
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    }
  }

  @Test
  fun testSignInStateChanged_onSigningIn() = runWithTestDispatcher {
    viewModel.navigationRequests.test {
      fakeAuthenticationManager.setState(SignInState.SigningIn)
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(MainUiState.OnUserSigningIn)
      verifyUserNotSaved()
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    }
  }

  // TODO: Add back testSignInStateChanged_onSignedIn_whenTosAcceptedAndActiveSurveyAvailable
  //   once reactivate last survey is implemented.
  // Issue URL: https://github.com/google/ground-android/issues/1612

  @Test
  fun testSignInStateChanged_onSignedIn_whenTosNotAccepted() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.termsOfService = Result.success(FakeData.TERMS_OF_SERVICE)

    viewModel.navigationRequests.test {
      fakeAuthenticationManager.signIn()
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(MainUiState.TosNotAccepted)
      verifyUserSaved()
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    }
  }

  @Test
  fun testSignInStateChanged_onSignedIn_getTos_whenTosMissing() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.termsOfService = null

    viewModel.navigationRequests.test {
      fakeAuthenticationManager.signIn()
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(MainUiState.TosNotAccepted)
      verifyUserSaved()
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    }
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

    viewModel.navigationRequests.test {
      fakeAuthenticationManager.signIn()
      advanceUntilIdle()
      // TODO: Update these implementation to make it clearer why this would be the case.
      // Issue URL: https://github.com/google/ground-android/issues/2667
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
      assertThat(awaitItem()).isEqualTo(MainUiState.TosNotAccepted)
    }
  }

  @Test
  fun testSignInStateChanged_onSignedIn_getTos_whenNotPermissionDeniedError() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = false
      fakeRemoteDataStore.termsOfService = Result.failure(Error("user error"))

      viewModel.navigationRequests.test {
        fakeAuthenticationManager.signIn()
        advanceUntilIdle()

        assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
        assertThat(awaitItem()).isEqualTo(MainUiState.TosNotAccepted)
      }
    }

  @Test
  fun testSignInStateChanged_onSignInError() = runWithTestDispatcher {
    setupUserPreferences()

    viewModel.navigationRequests.test {
      fakeAuthenticationManager.setState(SignInState.Error(Exception()))
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(MainUiState.OnUserSignedOut)
      verifyUserPreferencesCleared()
      verifyUserNotSaved()
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    }
  }
}
