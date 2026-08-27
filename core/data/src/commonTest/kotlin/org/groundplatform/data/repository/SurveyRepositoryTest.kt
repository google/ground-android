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

import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.groundplatform.data.FakeLocalSurveyStore
import org.groundplatform.data.FakeLocalValueStore
import org.groundplatform.data.FakeRemoteDataStore
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.Survey.GeneralAccess
import org.groundplatform.testing.FakeCrashLogger

@OptIn(ExperimentalCoroutinesApi::class)
class SurveyRepositoryTest {

  private val testScope = TestScope()
  private val fakeCrashLogger = FakeCrashLogger()
  private val fakeLocalSurveyStore = FakeLocalSurveyStore()
  private val fakeLocalValueStore = FakeLocalValueStore()
  private val fakeRemoteDataStore = FakeRemoteDataStore()

  private val repository =
    SurveyRepository(
      externalScope = testScope.backgroundScope,
      crashLogger = fakeCrashLogger,
      localSurveyStore = fakeLocalSurveyStore,
      localValueStore = fakeLocalValueStore,
      remoteDataStore = fakeRemoteDataStore,
    )

  private val testSurvey =
    Survey(
      id = "survey_123",
      title = "Test Survey",
      description = "Test Description",
      jobMap = emptyMap(),
      generalAccess = GeneralAccess.PUBLIC,
    )

  @Test
  fun activateSurvey_updatesActiveSurveyAndCrashLogger() = testScope.runTest {
    fakeLocalSurveyStore.insertOrUpdateSurvey(testSurvey)

    repository.activateSurvey(testSurvey.id)
    advanceUntilIdle()

    assertEquals(testSurvey, repository.activeSurvey)
    repository.activeSurveyFlow.test { assertEquals(testSurvey, expectMostRecentItem()) }
    assertEquals(testSurvey.id, fakeCrashLogger.lastSelectedSurveyId)
    assertEquals(testSurvey.id, fakeLocalValueStore.lastActiveSurveyId)
    assertTrue(repository.isSurveyActive(testSurvey.id))
  }

  @Test
  fun clearActiveSurvey_resetsActiveSurvey() = testScope.runTest {
    fakeLocalSurveyStore.insertOrUpdateSurvey(testSurvey)
    repository.activateSurvey(testSurvey.id)
    advanceUntilIdle()

    repository.clearActiveSurvey()
    advanceUntilIdle()

    assertNull(repository.activeSurvey)
    repository.activeSurveyFlow.test { assertNull(expectMostRecentItem()) }
    assertEquals("", fakeCrashLogger.lastSelectedSurveyId)
    assertEquals("", fakeLocalValueStore.lastActiveSurveyId)
    assertFalse(repository.isSurveyActive(testSurvey.id))
  }

  @Test
  fun saveSurvey_persistsToLocalSurveyStore() = testScope.runTest {
    repository.saveSurvey(testSurvey)

    assertEquals(testSurvey, fakeLocalSurveyStore.getSurveyById(testSurvey.id))
  }

  @Test
  fun getOfflineSurvey_returnsSavedSurvey() = testScope.runTest {
    fakeLocalSurveyStore.insertOrUpdateSurvey(testSurvey)

    assertEquals(testSurvey, repository.getOfflineSurvey(testSurvey.id))
  }

  @Test
  fun removeOfflineSurvey_deletesFromLocalSurveyStore() = testScope.runTest {
    fakeLocalSurveyStore.insertOrUpdateSurvey(testSurvey)
    assertEquals(testSurvey, repository.getOfflineSurvey(testSurvey.id))

    repository.removeOfflineSurvey(testSurvey.id)

    assertNull(repository.getOfflineSurvey(testSurvey.id))
  }

  @Test
  fun getRemoteSurvey_loadsFromRemoteDataStore() = testScope.runTest {
    fakeRemoteDataStore.surveys = listOf(testSurvey)

    val result = repository.getRemoteSurvey(testSurvey.id)
    assertEquals(testSurvey, result)
  }

  @Test
  fun subscribeToSurveyUpdates_delegatesToRemoteDataStore() = testScope.runTest {
    repository.subscribeToSurveyUpdates("survey_abc")

    assertTrue(fakeRemoteDataStore.subscribedSurveyUpdates.contains("survey_abc"))
  }

  @Test
  fun unsubscribeFromSurveyUpdates_delegatesToRemoteDataStore() = testScope.runTest {
    repository.unsubscribeFromSurveyUpdates("survey_abc")

    assertTrue(fakeRemoteDataStore.unsubscribedSurveyUpdates.contains("survey_abc"))
  }

  @Test
  fun dataSharingConsent_roundTrip() {
    assertFalse(repository.getDataSharingConsent(testSurvey.id))

    repository.setDataSharingConsent(testSurvey.id, true)
    assertTrue(repository.getDataSharingConsent(testSurvey.id))
  }
}
