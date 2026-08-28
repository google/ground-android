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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.groundplatform.data.FakeLocalLocationOfInterestStore
import org.groundplatform.data.FakeLocalSurveyStore
import org.groundplatform.data.FakeRemoteDataStore
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.locationofinterest.AuditInfo
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.testing.FakeAuthenticationManager
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeMutationSyncManager
import org.groundplatform.testing.FakeOfflineUuidGenerator
import org.groundplatform.testing.FakeUserRepository

class LocationOfInterestRepositoryTest {

  private val fakeSurveyStore = FakeLocalSurveyStore()
  private val fakeLoiStore = FakeLocalLocationOfInterestStore()
  private val fakeRemoteDataStore = FakeRemoteDataStore()
  private val fakeMutationSyncManager = FakeMutationSyncManager()
  private val fakeUserRepository = FakeUserRepository()
  private val fakeUuidGenerator = FakeOfflineUuidGenerator("test-loi-id")
  private val fakeAuthenticationManager = FakeAuthenticationManager()

  private lateinit var loiRepository: LocationOfInterestRepository

  @BeforeTest
  fun setUp() {
    loiRepository =
      LocationOfInterestRepository(
        localSurveyStore = fakeSurveyStore,
        localLoiStore = fakeLoiStore,
        remoteDataStore = fakeRemoteDataStore,
        mutationSyncManager = fakeMutationSyncManager,
        userRepository = fakeUserRepository,
        uuidGenerator = fakeUuidGenerator,
        authenticationManager = fakeAuthenticationManager,
      )
  }

  @Test
  fun saveLoi_createsMutationAndEnqueuesSync() = runTest {
    val point = Point(Coordinates(10.0, 20.0))
    val job = Job(id = "job1")

    val loiId =
      loiRepository.saveLoi(
        geometry = point,
        job = job,
        surveyId = "survey1",
        loiName = "My LOI",
        collectionId = "c1",
      )

    assertEquals("test-loi-id", loiId)
    assertEquals(1, fakeLoiStore.appliedMutations.size)
    val mutation = fakeLoiStore.appliedMutations.first()
    assertEquals("test-loi-id", mutation.locationOfInterestId)
    assertEquals(Mutation.Type.CREATE, mutation.type)
    assertEquals(1, fakeMutationSyncManager.enqueueSyncCount)
  }

  @Test
  fun getOfflineLoi_returnsLoiWhenFound() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey1")
    val loi = FakeDataGenerator.newLocationOfInterest(id = "loi1", surveyId = "survey1")
    fakeSurveyStore.insertOrUpdateSurvey(survey)
    fakeLoiStore.insertOrUpdate(loi)

    val result = loiRepository.getOfflineLoi("survey1", "loi1")

    assertEquals(loi, result)
  }

  @Test
  fun getOfflineLoi_returnsNullWhenNotFound() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey1")
    fakeSurveyStore.insertOrUpdateSurvey(survey)

    val result = loiRepository.getOfflineLoi("survey1", "nonexistent")

    assertNull(result)
  }

  @Test
  fun hasValidLois_returnsTrueWhenCountPositive() = runTest {
    val loi = FakeDataGenerator.newLocationOfInterest(id = "loi1", surveyId = "survey1")
    fakeLoiStore.insertOrUpdate(loi)

    assertTrue(loiRepository.hasValidLois("survey1"))
  }

  @Test
  fun hasValidLois_returnsFalseWhenCountZero() = runTest {
    assertFalse(loiRepository.hasValidLois("survey1"))
  }

  @Test
  fun getWithinBounds_filtersLoisWithinBounds() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey1")
    val insideLoi =
      FakeDataGenerator.newLocationOfInterest(
        id = "loi-inside",
        surveyId = "survey1",
        geometry = Point(Coordinates(10.0, 10.0)),
      )
    val outsideLoi =
      FakeDataGenerator.newLocationOfInterest(
        id = "loi-outside",
        surveyId = "survey1",
        geometry = Point(Coordinates(50.0, 50.0)),
      )
    fakeLoiStore.insertOrUpdate(insideLoi)
    fakeLoiStore.insertOrUpdate(outsideLoi)

    val bounds = Bounds(southwest = Coordinates(0.0, 0.0), northeast = Coordinates(20.0, 20.0))
    val result = loiRepository.getWithinBounds(survey, bounds).first()

    assertEquals(listOf(insideLoi), result)
  }

  @Test
  fun deleteLoi_failsForPredefinedLoi() = runTest {
    val loi =
      FakeDataGenerator.newLocationOfInterest(id = "loi1", surveyId = "survey1")
        .copy(isPredefined = true)

    assertFailsWith<IllegalStateException> { loiRepository.deleteLoi(loi) }
  }

  @Test
  fun deleteLoi_failsWhenUserNotOwnerOrOrganizer() = runTest {
    val owner = FakeDataGenerator.newUser(id = "owner1")
    val otherUser = FakeDataGenerator.newUser(id = "other1")
    fakeUserRepository.currentUser = otherUser

    val loi =
      FakeDataGenerator.newLocationOfInterest(
        id = "loi1",
        surveyId = "survey1",
        created = AuditInfo(owner),
      )
    val survey = FakeDataGenerator.newSurvey(id = "survey1", acl = emptyMap())
    fakeSurveyStore.insertOrUpdateSurvey(survey)

    assertFailsWith<IllegalStateException> { loiRepository.deleteLoi(loi) }
  }

  @Test
  fun deleteLoi_createsDeleteMutationAndEnqueuesSync() = runTest {
    val user = FakeDataGenerator.newUser(id = "user1")
    fakeUserRepository.currentUser = user

    val loi =
      FakeDataGenerator.newLocationOfInterest(
        id = "loi1",
        surveyId = "survey1",
        created = AuditInfo(user),
      )
    val survey = FakeDataGenerator.newSurvey(id = "survey1")
    fakeSurveyStore.insertOrUpdateSurvey(survey)

    loiRepository.deleteLoi(loi)

    assertEquals(1, fakeLoiStore.appliedMutations.size)
    val mutation = fakeLoiStore.appliedMutations.first()
    assertEquals(Mutation.Type.DELETE, mutation.type)
    assertEquals(1, fakeMutationSyncManager.enqueueSyncCount)
  }
}
