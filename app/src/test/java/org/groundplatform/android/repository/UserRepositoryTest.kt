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
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.local.stores.LocalUserStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.data.sync.MediaUploadWorkManager
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.domain.model.Role
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.auth.SignInState
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.model.settings.UserSettings
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
  @Inject lateinit var surveyRepository: SurveyRepositoryInterface
  @Inject lateinit var userRepository: UserRepositoryInterface
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @BindValue @Mock lateinit var networkManager: NetworkManager
  @BindValue @Mock lateinit var mediaUploadWorkManager: MediaUploadWorkManager

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
  fun `clearUserData() clears lastActiveSurveyId`() = runWithTestDispatcher {
    localValueStore.lastActiveSurveyId = "foo"

    userRepository.clearUserData()

    assertThat(localValueStore.lastActiveSurveyId).isEmpty()
  }

  @Test
  fun `clearUserData() clears the local database`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(FakeData.SURVEY)

    userRepository.clearUserData()

    assertThat(localSurveyStore.surveys.first()).isEmpty()
  }

  @Test
  fun `clearUserData() retains an already consumed deferred deep link`() = runWithTestDispatcher {
    localValueStore.isDeferredDeeplinkConsumed = true

    userRepository.clearUserData()

    assertThat(localValueStore.isDeferredDeeplinkConsumed).isTrue()
  }

  @Test
  fun `clearUserData() leaves an unconsumed deferred deep link unconsumed`() =
    runWithTestDispatcher {
      localValueStore.isDeferredDeeplinkConsumed = false

      userRepository.clearUserData()

      assertThat(localValueStore.isDeferredDeeplinkConsumed).isFalse()
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

  @Test
  fun `canUserSubmitData() returns true when general access is PUBLIC`() = runWithTestDispatcher {
    val survey = FakeData.SURVEY.copy(generalAccess = Survey.GeneralAccess.PUBLIC)
    localSurveyStore.insertOrUpdateSurvey(survey)
    surveyRepository.activateSurvey(survey.id)

    assertThat(userRepository.canUserSubmitData()).isTrue()
  }

  @Test
  fun `canUserSubmitData() returns true when general access is UNLISTED`() = runWithTestDispatcher {
    val survey = FakeData.SURVEY.copy(generalAccess = Survey.GeneralAccess.UNLISTED)
    localSurveyStore.insertOrUpdateSurvey(survey)
    surveyRepository.activateSurvey(survey.id)

    assertThat(userRepository.canUserSubmitData()).isTrue()
  }

  @Test
  fun `canUserSubmitData() returns false when user role is VIEWER`() = runWithTestDispatcher {
    val user = FakeData.USER
    val survey = FakeData.SURVEY.copy(acl = mapOf(user.email to Role.VIEWER.toString()))
    fakeAuthenticationManager.setUser(user)
    localSurveyStore.insertOrUpdateSurvey(survey)
    surveyRepository.activateSurvey(survey.id)

    assertThat(userRepository.canUserSubmitData()).isFalse()
  }

  @Test
  fun `getUserSettings() returns correct settings`() {
    localValueStore.selectedLanguage = "fr"
    localValueStore.selectedLengthUnit = MeasurementUnits.IMPERIAL.name
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true

    val settings = userRepository.getUserSettings()

    assertThat(settings.language).isEqualTo("fr")
    assertThat(settings.measurementUnits).isEqualTo(MeasurementUnits.IMPERIAL)
    assertThat(settings.shouldUploadPhotosOnWifiOnly).isTrue()
  }

  @Test
  fun `setUserSettings() updates local store`() {
    val settings = UserSettings("fr", MeasurementUnits.IMPERIAL, true)

    userRepository.setUserSettings(settings)

    assertThat(localValueStore.selectedLanguage).isEqualTo("fr")
    assertThat(localValueStore.selectedLengthUnit).isEqualTo(MeasurementUnits.IMPERIAL.name)
    assertThat(localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly).isTrue()
  }

  @Test
  fun `setUserSettings() reschedules media uploads when wifi only uploads are disabled`() {
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true

    userRepository.setUserSettings(
      UserSettings("fr", MeasurementUnits.IMPERIAL, shouldUploadPhotosOnWifiOnly = false)
    )

    verify(mediaUploadWorkManager).rescheduleSyncWorker()
  }

  @Test
  fun `setUserSettings() reschedules media uploads when wifi only uploads are enabled`() {
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = false

    userRepository.setUserSettings(
      UserSettings("fr", MeasurementUnits.IMPERIAL, shouldUploadPhotosOnWifiOnly = true)
    )

    verify(mediaUploadWorkManager).rescheduleSyncWorker()
  }

  @Test
  fun `setUserSettings() doesn't reschedule media uploads when the network preference is unchanged`() {
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true

    userRepository.setUserSettings(
      UserSettings("fr", MeasurementUnits.IMPERIAL, shouldUploadPhotosOnWifiOnly = true)
    )

    verify(mediaUploadWorkManager, never()).rescheduleSyncWorker()
  }

  @Test
  fun `setUserSettings() doesn't reschedule media uploads when only the language changes`() {
    // The store defaults to uploading over any connection, so this covers the common case of
    // editing an unrelated setting without ever having touched the upload preference.
    userRepository.setUserSettings(
      UserSettings("fr", MeasurementUnits.IMPERIAL, shouldUploadPhotosOnWifiOnly = false)
    )

    verify(mediaUploadWorkManager, never()).rescheduleSyncWorker()
  }

  @Test
  fun `setUserSettings() persists the new preference before rescheduling media uploads`() {
    // Rescheduling first would re-enqueue the worker with the previous network type, leaving
    // uploads stuck exactly as reported in issue 3945.
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = true
    var preferenceWhenRescheduled: Boolean? = null
    whenever(mediaUploadWorkManager.rescheduleSyncWorker()).then {
      preferenceWhenRescheduled = localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly
    }

    userRepository.setUserSettings(
      UserSettings("fr", MeasurementUnits.IMPERIAL, shouldUploadPhotosOnWifiOnly = false)
    )

    assertThat(preferenceWhenRescheduled).isFalse()
  }

  @Test
  fun `clearUserData() cancels pending media uploads`() = runWithTestDispatcher {
    userRepository.clearUserData()

    verify(mediaUploadWorkManager).cancelSyncWorker()
  }
}
