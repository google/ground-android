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
package org.groundplatform.android.ui.main

import android.content.SharedPreferences
import androidx.core.content.edit
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.local.room.LocalDataStoreException
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.data.remote.UpdateRequiredException
import org.groundplatform.android.di.AppConfigRepositoryModule
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.system.deeplink.PlayInstallReferrerService
import org.groundplatform.domain.model.AppConfig
import org.groundplatform.domain.model.auth.SignInState
import org.groundplatform.domain.repository.AppConfigRepositoryInterface
import org.groundplatform.domain.repository.TermsOfServiceRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@UninstallModules(AppConfigRepositoryModule::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest : BaseHiltTest() {

  @BindValue @JvmField val playInstallReferrerService: PlayInstallReferrerService = mock()
  @BindValue @Mock lateinit var appConfigRepository: AppConfigRepositoryInterface

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var localValueStore: LocalValueStore
  @Inject lateinit var viewModel: MainViewModel
  @Inject lateinit var sharedPreferences: SharedPreferences
  @Inject lateinit var tosRepository: TermsOfServiceRepositoryInterface
  @Inject lateinit var userRepository: UserRepositoryInterface

  @Before
  override fun setUp() {
    super.setUp()

    fakeAuthenticationManager.setUser(FakeData.USER)
    whenever(appConfigRepository.getAppConfig()).thenReturn(NO_UPDATE)
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
  fun `sign in updated on sign out`() = runWithTestDispatcher {
    setupUserPreferences()

    viewModel.uiEffects.test {
      fakeAuthenticationManager.signOut()
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(MainUiEffect.SignedOut)
      verifyUserPreferencesCleared()
      verifyUserNotSaved()
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    }
  }

  @Test
  fun `navigation redirects to signing in state when authentication in progress`() =
    runWithTestDispatcher {
      viewModel.uiEffects.test {
        fakeAuthenticationManager.setState(SignInState.SigningIn)
        advanceUntilIdle()

        expectNoEvents()
        verifyUserNotSaved()
        assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
      }
    }

  @Test
  fun `navigation redirects to TOS screen when signed in but terms not accepted`() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = false
      fakeRemoteDataStore.termsOfService = Result.success(FakeData.TERMS_OF_SERVICE)

      viewModel.uiEffects.test {
        fakeAuthenticationManager.signIn()
        advanceUntilIdle()

        assertThat(awaitItem())
          .isEqualTo(
            MainUiEffect.OpenStartDestination(MainUiEffect.StartDestination.TermsOfService)
          )
        verifyUserSaved()
        assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
      }
    }

  @Test
  fun `navigation redirects to TOS screen when terms document missing`() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.termsOfService = null

    viewModel.uiEffects.test {
      fakeAuthenticationManager.signIn()
      advanceUntilIdle()

      assertThat(awaitItem())
        .isEqualTo(MainUiEffect.OpenStartDestination(MainUiEffect.StartDestination.TermsOfService))
      verifyUserSaved()
      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
    }
  }

  @Test
  fun `navigation redirects to TOS screen when permission denied for terms`() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = false
      fakeRemoteDataStore.termsOfService =
        Result.failure(
          FirebaseFirestoreException(
            "permission denied",
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
          )
        )

      viewModel.uiEffects.test {
        fakeAuthenticationManager.signIn()
        advanceUntilIdle()
        // TODO: Update these implementation to make it clearer why this would be the case.
        // Issue URL: https://github.com/google/ground-android/issues/2667
        assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
        assertThat(awaitItem())
          .isEqualTo(
            MainUiEffect.OpenStartDestination(MainUiEffect.StartDestination.TermsOfService)
          )
      }
    }

  @Test
  fun `navigation redirects to TOS screen when error retrieving terms`() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = false
    fakeRemoteDataStore.termsOfService = Result.failure(Error("user error"))

    viewModel.uiEffects.test {
      fakeAuthenticationManager.signIn()
      advanceUntilIdle()

      assertThat(tosRepository.isTermsOfServiceAccepted).isFalse()
      assertThat(awaitItem())
        .isEqualTo(MainUiEffect.OpenStartDestination(MainUiEffect.StartDestination.TermsOfService))
    }
  }

  @Test
  fun `navigation redirects to deferred deep link survey when install referrer has survey id`() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = true
      whenever(playInstallReferrerService.getDeferredSurveyId()).thenReturn(SURVEY_ID)

      viewModel.uiEffects.test {
        fakeAuthenticationManager.signIn()
        advanceUntilIdle()

        assertThat(awaitItem())
          .isEqualTo(
            MainUiEffect.OpenStartDestination(MainUiEffect.StartDestination.ActiveSurvey(SURVEY_ID))
          )
      }
    }

  @Test
  fun `navigation falls back to survey selector when install referrer has no survey id`() =
    runWithTestDispatcher {
      tosRepository.isTermsOfServiceAccepted = true
      whenever(playInstallReferrerService.getDeferredSurveyId()).thenReturn(null)

      viewModel.uiEffects.test {
        fakeAuthenticationManager.signIn()
        advanceUntilIdle()

        assertThat(awaitItem())
          .isEqualTo(
            MainUiEffect.OpenStartDestination(MainUiEffect.StartDestination.SurveySelector)
          )
      }
    }

  @Test
  fun `stays signed in when an app update is required`() = runWithTestDispatcher {
    tosRepository.isTermsOfServiceAccepted = true
    localValueStore.lastActiveSurveyId = SURVEY_ID
    fakeRemoteDataStore.onLoadSurvey = { throw UpdateRequiredException() }

    viewModel.uiEffects.test {
      fakeAuthenticationManager.signIn()
      advanceUntilIdle()

      expectNoEvents()
      verifyUserSaved()
    }
  }

  @Test
  fun `isAppUpdateRequired reflects the active config`() {
    assertThat(viewModel.isAppUpdateRequired()).isFalse()

    whenever(appConfigRepository.getAppConfig()).thenReturn(UPDATE_REQUIRED)

    assertThat(viewModel.isAppUpdateRequired()).isTrue()
  }

  companion object {
    private const val SURVEY_ID = "survey_123"
    private val NO_UPDATE = AppConfig(minAppVersion = "0.0.0", forceUpdate = false)
    private val UPDATE_REQUIRED = AppConfig(minAppVersion = "9999.0.0", forceUpdate = true)
  }
}
