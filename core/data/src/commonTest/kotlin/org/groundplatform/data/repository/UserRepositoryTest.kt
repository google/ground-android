/*
 * Copyright 2026 Google LLC
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

package org.groundplatform.data.repository

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.groundplatform.data.FakeLocalDatabase
import org.groundplatform.data.FakeLocalUserStore
import org.groundplatform.data.FakeLocalValueStore
import org.groundplatform.data.FakeRemoteDataStore
import org.groundplatform.domain.model.Role
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.auth.SignInState
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.locationofinterest.AuditInfo
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.model.settings.UserSettings
import org.groundplatform.domain.system.NetworkStatus
import org.groundplatform.testing.FakeAuthenticationManager
import org.groundplatform.testing.FakeNetworkManager
import org.groundplatform.testing.FakeSurveyRepository

class UserRepositoryTest {

  private val testUser = User(id = "user1", email = "user1@gmail.com", displayName = "User 1")
  private val fakeAuthManager = FakeAuthenticationManager(testUser)
  private val fakeValueStore = FakeLocalValueStore()
  private val fakeUserStore = FakeLocalUserStore()
  private val fakeNetworkManager = FakeNetworkManager()
  private val fakeSurveyRepository = FakeSurveyRepository()
  private val fakeRemoteDataStore = FakeRemoteDataStore()
  private val fakeLocalDatabase = FakeLocalDatabase()

  private lateinit var userRepository: UserRepository

  @BeforeTest
  fun setUp() {
    userRepository =
      UserRepository(
        authenticationManager = fakeAuthManager,
        localValueStore = fakeValueStore,
        localUserStore = fakeUserStore,
        networkManager = fakeNetworkManager,
        surveyRepository = fakeSurveyRepository,
        remoteDataStore = fakeRemoteDataStore,
        localDatabase = fakeLocalDatabase,
      )
  }

  @Test
  fun getAuthenticatedUser_returnsUserFromAuthenticationManager() = runTest {
    assertEquals(testUser, userRepository.getAuthenticatedUser())
  }

  @Test
  fun getSignInState_returnsFlowFromAuthenticationManager() = runTest {
    assertEquals(SignInState.SignedIn(testUser), userRepository.getSignInState().first())
  }

  @Test
  fun init_delegatesToAuthenticationManager() {
    userRepository.init()
    assertTrue(fakeAuthManager.initCalled)
  }

  @Test
  fun signIn_delegatesToAuthenticationManager() {
    userRepository.signIn()
    assertTrue(fakeAuthManager.signInCalled)
  }

  @Test
  fun signOut_delegatesToAuthenticationManager() {
    userRepository.signOut()
    assertTrue(fakeAuthManager.signOutCalled)
  }

  @Test
  fun saveUserDetails_savesLocallyAndRefreshesProfileWhenOnline() = runTest {
    fakeNetworkManager.setNetworkStatus(NetworkStatus.AVAILABLE)

    userRepository.saveUserDetails(testUser)

    assertEquals(testUser, fakeUserStore.getUser(testUser.id))
    assertEquals(1, fakeRemoteDataStore.refreshUserProfileCount)
  }

  @Test
  fun saveUserDetails_savesLocallyAndSkipsRemoteRefreshWhenOffline() = runTest {
    fakeNetworkManager.setNetworkStatus(NetworkStatus.UNAVAILABLE)

    userRepository.saveUserDetails(testUser)

    assertEquals(testUser, fakeUserStore.getUser(testUser.id))
    assertEquals(0, fakeRemoteDataStore.refreshUserProfileCount)
  }

  @Test
  fun getUser_returnsUserFromLocalUserStore() = runTest {
    fakeUserStore.insertOrUpdateUser(testUser)
    assertEquals(testUser, userRepository.getUser(testUser.id))
  }

  @Test
  fun clearUserData_clearsValueStoreAndLocalDatabaseTables() = runTest {
    fakeValueStore.selectedLanguage = "es"
    fakeValueStore.isDeferredDeeplinkConsumed = false

    userRepository.clearUserData()

    assertTrue(fakeLocalDatabase.clearAllTablesCalled)
    assertEquals("en", fakeValueStore.selectedLanguage)
  }

  @Test
  fun clearUserData_preservesIsDeferredDeeplinkConsumedWhenTrue() = runTest {
    fakeValueStore.isDeferredDeeplinkConsumed = true

    userRepository.clearUserData()

    assertTrue(fakeLocalDatabase.clearAllTablesCalled)
    assertTrue(fakeValueStore.isDeferredDeeplinkConsumed)
  }

  @Test
  fun canUserSubmitData_returnsTrueForPublicSurvey() = runTest {
    val survey =
      Survey(
        id = "s1",
        title = "Public Survey",
        description = "Description",
        jobMap = emptyMap(),
        generalAccess = Survey.GeneralAccess.PUBLIC,
      )
    fakeSurveyRepository.saveSurvey(survey)
    fakeSurveyRepository.activateSurvey("s1")

    assertTrue(userRepository.canUserSubmitData())
  }

  @Test
  fun canUserSubmitData_returnsFalseForViewerRole() = runTest {
    val survey =
      Survey(
        id = "s2",
        title = "Restricted Survey",
        description = "Description",
        jobMap = emptyMap(),
        acl = mapOf(testUser.email to Role.VIEWER.name.lowercase()),
        generalAccess = Survey.GeneralAccess.RESTRICTED,
      )
    fakeSurveyRepository.saveSurvey(survey)
    fakeSurveyRepository.activateSurvey("s2")

    assertFalse(userRepository.canUserSubmitData())
  }

  @Test
  fun canDeleteLoi_returnsTrueForOwnerAndFalseForPredefined() = runTest {
    val userLoi =
      LocationOfInterest(
        id = "loi1",
        surveyId = "s1",
        created = AuditInfo(testUser),
        lastModified = AuditInfo(testUser),
        geometry = Point(Coordinates(0.0, 0.0)),
        job = Job(id = "job1"),
        isPredefined = false,
      )
    val predefinedLoi = userLoi.copy(id = "loi2", isPredefined = true)

    assertTrue(userRepository.canDeleteLoi(userLoi))
    assertFalse(userRepository.canDeleteLoi(predefinedLoi))
  }

  @Test
  fun userSettings_getAndSetUpdateLocalValueStore() {
    val settings =
      UserSettings(
        language = "fr",
        measurementUnits = MeasurementUnits.METRIC,
        shouldUploadPhotosOnWifiOnly = true,
      )

    userRepository.setUserSettings(settings)

    assertEquals(settings, userRepository.getUserSettings())
    assertEquals("fr", fakeValueStore.selectedLanguage)
    assertEquals(MeasurementUnits.METRIC.name, fakeValueStore.selectedLengthUnit)
    assertTrue(fakeValueStore.shouldUploadMediaOverUnmeteredConnectionOnly)
  }
}
