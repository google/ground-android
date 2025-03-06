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
package org.groundplatform.android.repository

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.model.Role
import org.groundplatform.android.persistence.local.LocalValueStore
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.local.stores.LocalUserStore
import org.groundplatform.android.persistence.remote.FakeRemoteDataStore
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.system.auth.SignInState
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class UserRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var localUserStore: LocalUserStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localValueStore: LocalValueStore
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var userRepository: UserRepository
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @BindValue @Mock lateinit var networkManager: NetworkManager

  @Test
  fun `currentUser returns current user`() = runWithTestDispatcher {
    fakeAuthenticationManager.setUser(FakeData.USER)

    assertThat(userRepository.getAuthenticatedUser()).isEqualTo(FakeData.USER)
  }

  @Test
  fun `saveUserDetails() updates local user profile`() = runWithTestDispatcher {
    whenever(networkManager.isNetworkConnected()).thenReturn(true)

    userRepository.saveUserDetails(FakeData.USER)

    assertThat(localUserStore.getUser(FakeData.USER.id)).isEqualTo(FakeData.USER)
  }

  @Test
  fun `saveUserDetails() updates remote user profile`() = runWithTestDispatcher {
    whenever(networkManager.isNetworkConnected()).thenReturn(true)

    userRepository.saveUserDetails(FakeData.USER)

    assertThat(fakeRemoteDataStore.userProfileRefreshCount).isEqualTo(1)
  }

  @Test
  fun `saveUserDetails() doesn't update remote user profile when offline `() =
    runWithTestDispatcher {
      whenever(networkManager.isNetworkConnected()).thenReturn(false)

      userRepository.saveUserDetails(FakeData.USER)

      assertThat(fakeRemoteDataStore.userProfileRefreshCount).isEqualTo(0)
    }

  @Test
  fun `clearUserPreferences() clears lastActiveSurveyId`() {
    localValueStore.lastActiveSurveyId = "foo"

    userRepository.clearUserPreferences()

    assertThat(localValueStore.lastActiveSurveyId).isEmpty()
  }

  @Test
  fun `canUserSubmitData() when user has permissions returns true`() = runWithTestDispatcher {
    val user = FakeData.USER
    val survey = FakeData.SURVEY.copy(acl = mapOf(Pair(user.email, Role.OWNER.toString())))
    fakeAuthenticationManager.setUser(user)
    localSurveyStore.insertOrUpdateSurvey(survey)
    surveyRepository.activateSurvey(survey.id)

    assertThat(userRepository.canUserSubmitData()).isTrue()
  }

  @Test
  fun `canUserSubmitData() when user doesn't have permissions returns false`() =
    runWithTestDispatcher {
      val user = FakeData.USER
      val survey = FakeData.SURVEY.copy(acl = mapOf())
      fakeAuthenticationManager.setUser(user)
      localSurveyStore.insertOrUpdateSurvey(survey)
      surveyRepository.activateSurvey(survey.id)
      advanceUntilIdle()

      assertThat(userRepository.canUserSubmitData()).isFalse()
    }

  @Test
  fun `signOut() should sign out the user`() {
    runWithTestDispatcher {
      fakeAuthenticationManager.setUser(FakeData.USER)
      fakeAuthenticationManager.setState(SignInState.SignedIn(FakeData.USER))

      userRepository.signOut()
      assertThat(fakeAuthenticationManager.signInState.first()).isEqualTo(SignInState.SignedOut)
    }
  }

  @Test
  fun `canUserSubmitData() when user email is empty returns false`() = runWithTestDispatcher {
    val user = FakeData.USER.copy(email = "")
    val survey = FakeData.SURVEY.copy(acl = mapOf(Pair("user@gmail.com", Role.OWNER.toString())))
    fakeAuthenticationManager.setUser(user)
    localSurveyStore.insertOrUpdateSurvey(survey)
    surveyRepository.activateSurvey(survey.id)
    advanceUntilIdle()

    assertThat(userRepository.canUserSubmitData()).isFalse()
  }
}
