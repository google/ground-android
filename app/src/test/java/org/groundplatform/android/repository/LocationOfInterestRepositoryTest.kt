/*
 * Copyright 2020 Google LLC
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

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.data.sync.MutationSyncWorkManager
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.mutation.Mutation.Type.CREATE
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.MutationRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.domain.usecases.survey.SyncSurveyUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

/** Distinguishes a deliberately failed fetch from any other error the sync might raise. */
private class TestSyncException : RuntimeException("fetch failed")

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocationOfInterestRepositoryTest : BaseHiltTest() {
  @BindValue @Mock lateinit var mockWorkManager: MutationSyncWorkManager

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var locationOfInterestRepository: LocationOfInterestRepositoryInterface
  @Inject lateinit var localLoiStore: LocalLocationOfInterestStore
  @Inject lateinit var mutationRepository: MutationRepositoryInterface
  @Inject lateinit var userRepository: UserRepositoryInterface
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var syncSurvey: SyncSurveyUseCase

  private val mutation = LOCATION_OF_INTEREST.toMutation(CREATE, TEST_USER.id)

  @Before
  override fun setUp() {
    super.setUp()
    runWithTestDispatcher {
      // Setup user
      fakeAuthenticationManager.setUser(TEST_USER)
      userRepository.saveUserDetails(TEST_USER)

      // Setup survey and LOIs
      fakeRemoteDataStore.surveys = listOf(TEST_SURVEY)
      fakeRemoteDataStore.predefinedLois = TEST_LOCATIONS_OF_INTEREST
      activateSurvey(TEST_SURVEY.id)
      advanceUntilIdle()
    }
  }

  @Test
  fun `apply and enqueue when creates local loi`() = runWithTestDispatcher {
    // TODO: Remove once customId and caption are handled consistently.
    // Issue URL: https://github.com/google/ground-android/issues/1559
    val loi =
      LOCATION_OF_INTEREST.copy(
        customId = "",
        // TODO: Remove once creation time is preserved in local db.
        // Issue URL: https://github.com/google/ground-android/issues/1562
        lastModified = LOCATION_OF_INTEREST.created,
      )
    locationOfInterestRepository.applyAndEnqueue(loi.toMutation(CREATE, TEST_USER.id))

    assertThat(locationOfInterestRepository.getOfflineLoi(TEST_SURVEY.id, loi.id)).isEqualTo(loi)
  }

  @Test
  fun `apply and enqueue when enqueues loi mutation`() = runWithTestDispatcher {
    locationOfInterestRepository.applyAndEnqueue(mutation)

    mutationRepository.getUploadQueueFlow().test {
      with(expectMostRecentItem().first()) {
        assertThat(userId).isEqualTo(TEST_USER.id)
        assertThat(clientTimestamp).isEqualTo(mutation.clientTimestamp)
        assertThat(uploadStatus).isEqualTo(mutation.syncStatus)
        assertThat(loiMutation).isEqualTo(mutation.copy(id = 1))
        assertThat(submissionMutation).isNull()
      }
    }
  }

  @Test
  fun `apply and enqueue when enqueues worker`() = runWithTestDispatcher {
    locationOfInterestRepository.applyAndEnqueue(mutation)

    verify(mockWorkManager).enqueueSyncWorker()
  }

  @Test
  fun `apply and enqueue when returns error on worker sync failure`() = runWithTestDispatcher {
    `when`(mockWorkManager.enqueueSyncWorker()).thenThrow(Error())

    assertFailsWith<Error> {
      locationOfInterestRepository.applyAndEnqueue(
        LOCATION_OF_INTEREST.toMutation(CREATE, TEST_USER.id)
      )
    }

    verify(mockWorkManager, times(1)).enqueueSyncWorker()
  }

  @Test
  fun `sync saves every page of a multi page source`() = runWithTestDispatcher {
    fakeRemoteDataStore.predefinedLoiPages =
      flowOf(
        listOf(TEST_POINT_OF_INTEREST_1, TEST_POINT_OF_INTEREST_2),
        listOf(TEST_POINT_OF_INTEREST_3),
        listOf(TEST_AREA_OF_INTEREST_1, TEST_AREA_OF_INTEREST_2),
      )

    locationOfInterestRepository.syncLocationsOfInterest(TEST_SURVEY)

    assertThat(locationOfInterestRepository.getValidLois(TEST_SURVEY).first())
      .containsExactlyElementsIn(TEST_LOCATIONS_OF_INTEREST)
  }

  @Test
  fun `sync saves each page before requesting the next`() = runWithTestDispatcher {
    localLoiStore.deleteNotIn(TEST_SURVEY.id, emptyList())
    val savedWhenPageRequested = mutableListOf<Int>()

    fakeRemoteDataStore.predefinedLoiPages = flow {
      savedWhenPageRequested += localLoiStore.getLoiCount(TEST_SURVEY.id)
      emit(listOf(TEST_POINT_OF_INTEREST_1, TEST_POINT_OF_INTEREST_2))
      savedWhenPageRequested += localLoiStore.getLoiCount(TEST_SURVEY.id)
      emit(listOf(TEST_POINT_OF_INTEREST_3))
      savedWhenPageRequested += localLoiStore.getLoiCount(TEST_SURVEY.id)
    }

    locationOfInterestRepository.syncLocationsOfInterest(TEST_SURVEY)

    assertThat(savedWhenPageRequested).containsExactly(0, 2, 3).inOrder()
  }

  @Test
  fun `sync that fails midway keeps saved pages and deletes nothing`() = runWithTestDispatcher {
    val newLoi = createPoint("6", COORDINATE_2)
    fakeRemoteDataStore.predefinedLoiPages = flow {
      emit(listOf(newLoi))
      throw TestSyncException()
    }

    assertFailsWith<TestSyncException> {
      locationOfInterestRepository.syncLocationsOfInterest(TEST_SURVEY)
    }

    val lois = locationOfInterestRepository.getValidLois(TEST_SURVEY).first()
    assertThat(lois).contains(newLoi)
    assertThat(lois).containsAtLeastElementsIn(TEST_LOCATIONS_OF_INTEREST)
  }

  @Test
  fun `sync deletes only the lois absent from every page`() = runWithTestDispatcher {
    // Every LOI is stored to begin with, having been synced during setup.
    assertThat(locationOfInterestRepository.getValidLois(TEST_SURVEY).first())
      .containsExactlyElementsIn(TEST_LOCATIONS_OF_INTEREST)

    // Sync again, with the server now returning two of them across separate pages.
    fakeRemoteDataStore.predefinedLoiPages =
      flowOf(listOf(TEST_POINT_OF_INTEREST_1), listOf(TEST_AREA_OF_INTEREST_2))
    locationOfInterestRepository.syncLocationsOfInterest(TEST_SURVEY)

    assertThat(locationOfInterestRepository.getValidLois(TEST_SURVEY).first())
      .containsExactly(TEST_POINT_OF_INTEREST_1, TEST_AREA_OF_INTEREST_2)
  }

  @Test
  fun `sync updates lois that changed remotely`() = runWithTestDispatcher {
    val updated = TEST_POINT_OF_INTEREST_1.copy(geometry = Point(COORDINATE_3))
    fakeRemoteDataStore.predefinedLois = TEST_LOCATIONS_OF_INTEREST.map {
      if (it.id == updated.id) updated else it
    }

    locationOfInterestRepository.syncLocationsOfInterest(TEST_SURVEY)

    val lois = locationOfInterestRepository.getValidLois(TEST_SURVEY).first()
    assertThat(lois).contains(updated)
    assertThat(lois).doesNotContain(TEST_POINT_OF_INTEREST_1)
  }

  @Test
  fun `sync does not delete lois with pending mutations`() = runWithTestDispatcher {
    // Created locally and not yet uploaded, so the server cannot know about it.
    val pending =
      LOCATION_OF_INTEREST.copy(customId = "", lastModified = LOCATION_OF_INTEREST.created)
    locationOfInterestRepository.applyAndEnqueue(pending.toMutation(CREATE, TEST_USER.id))

    fakeRemoteDataStore.predefinedLois = listOf(TEST_POINT_OF_INTEREST_1)
    locationOfInterestRepository.syncLocationsOfInterest(TEST_SURVEY)

    assertThat(locationOfInterestRepository.getOfflineLoi(TEST_SURVEY.id, pending.id))
      .isEqualTo(pending)
  }

  @Test
  fun `loi within bounds when out of bounds returns empty list`() = runWithTestDispatcher {
    val southwest = Coordinates(-60.0, -60.0)
    val northeast = Coordinates(-50.0, -50.0)

    locationOfInterestRepository.getWithinBounds(TEST_SURVEY, Bounds(southwest, northeast)).test {
      assertThat(expectMostRecentItem()).isEmpty()
    }
  }

  @Test
  fun `loi within bounds when some lo is inside bounds returns partial list`() =
    runWithTestDispatcher {
      val southwest = Coordinates(-20.0, -20.0)
      val northeast = Coordinates(-10.0, -10.0)

      locationOfInterestRepository.getWithinBounds(TEST_SURVEY, Bounds(southwest, northeast)).test {
        assertThat(expectMostRecentItem())
          .isEqualTo(listOf(TEST_POINT_OF_INTEREST_1, TEST_AREA_OF_INTEREST_1))
      }
    }

  @Test
  fun `loi within bounds when all lo is inside bounds returns complete list`() =
    runWithTestDispatcher {
      val southwest = Coordinates(-20.0, -20.0)
      val northeast = Coordinates(20.0, 20.0)

      locationOfInterestRepository.getWithinBounds(TEST_SURVEY, Bounds(southwest, northeast)).test {
        assertThat(expectMostRecentItem())
          .isEqualTo(
            listOf(
              TEST_POINT_OF_INTEREST_1,
              TEST_POINT_OF_INTEREST_2,
              TEST_POINT_OF_INTEREST_3,
              TEST_AREA_OF_INTEREST_1,
              TEST_AREA_OF_INTEREST_2,
            )
          )
      }
    }

  @Test
  fun `hasValidLois when survey has no lois returns false`() = runWithTestDispatcher {
    // Remove all LOIs from local db inserted during setup()
    localLoiStore.deleteNotIn(TEST_SURVEY.id, emptyList())

    assertThat(locationOfInterestRepository.hasValidLois(TEST_SURVEY.id)).isFalse()
  }

  @Test
  fun `hasValidLois when survey has lois returns true`() = runWithTestDispatcher {
    // Remove all LOIs from local db inserted during setup()
    localLoiStore.deleteNotIn(TEST_SURVEY.id, emptyList())

    // Insert a new LOI
    locationOfInterestRepository.applyAndEnqueue(
      LOCATION_OF_INTEREST.toMutation(CREATE, TEST_USER.id)
    )

    assertThat(locationOfInterestRepository.hasValidLois(TEST_SURVEY.id)).isTrue()
  }

  @Test
  fun `should load all types of LOIs when visibility is ALL_SURVEY_PARTICIPANTS`() =
    runWithTestDispatcher {
      val survey = TEST_SURVEY.copy(dataVisibility = Survey.DataVisibility.ALL_SURVEY_PARTICIPANTS)
      fakeRemoteDataStore.surveys = listOf(survey)

      val predefinedLoi = FakeData.LOCATION_OF_INTEREST.copy(id = "predefined_id")
      val userLoi = FakeData.LOCATION_OF_INTEREST.copy(id = "user_id")
      val sharedLoi = FakeData.LOCATION_OF_INTEREST.copy(id = "shared_id")
      fakeRemoteDataStore.predefinedLois = listOf(predefinedLoi)
      fakeRemoteDataStore.userLois = listOf(userLoi)
      fakeRemoteDataStore.sharedLois = listOf(sharedLoi)

      val expected = setOf(predefinedLoi, userLoi, sharedLoi)

      syncSurvey(survey.id)

      val actual = locationOfInterestRepository.getValidLois(survey).first()

      assertThat(actual).isEqualTo(expected)
    }

  @Test
  fun `should not load shared LOIs when visibility is not ALL_SURVEY_PARTICIPANTS`() =
    runWithTestDispatcher {
      val survey =
        TEST_SURVEY.copy(dataVisibility = Survey.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS)
      fakeRemoteDataStore.surveys = listOf(survey)

      val predefinedLoi = FakeData.LOCATION_OF_INTEREST.copy(id = "predefined_id")
      val userLoi = FakeData.LOCATION_OF_INTEREST.copy(id = "user_id")
      fakeRemoteDataStore.predefinedLois = listOf(predefinedLoi)
      fakeRemoteDataStore.userLois = listOf(userLoi)

      val expected = setOf(predefinedLoi, userLoi)

      syncSurvey(survey.id)

      val actual = locationOfInterestRepository.getValidLois(survey).first()

      assertThat(actual).isEqualTo(expected)
    }

  companion object {
    private val COORDINATE_1 = Coordinates(-20.0, -20.0)
    private val COORDINATE_2 = Coordinates(0.0, 0.0)
    private val COORDINATE_3 = Coordinates(20.0, 20.0)

    private val AREA_OF_INTEREST = FakeData.AREA_OF_INTEREST
    private val LOCATION_OF_INTEREST = FakeData.LOCATION_OF_INTEREST
    private val TEST_SURVEY = FakeData.SURVEY
    private val TEST_USER = FakeData.USER

    private val TEST_POINT_OF_INTEREST_1 = createPoint("1", COORDINATE_1)
    private val TEST_POINT_OF_INTEREST_2 = createPoint("2", COORDINATE_2)
    private val TEST_POINT_OF_INTEREST_3 = createPoint("3", COORDINATE_3)
    private val TEST_AREA_OF_INTEREST_1 =
      createPolygon("4", listOf(COORDINATE_1, COORDINATE_2, COORDINATE_1))
    private val TEST_AREA_OF_INTEREST_2 =
      createPolygon("5", listOf(COORDINATE_2, COORDINATE_3, COORDINATE_2))

    private val TEST_LOCATIONS_OF_INTEREST =
      listOf(
        TEST_POINT_OF_INTEREST_1,
        TEST_POINT_OF_INTEREST_2,
        TEST_POINT_OF_INTEREST_3,
        TEST_AREA_OF_INTEREST_1,
        TEST_AREA_OF_INTEREST_2,
      )

    private fun createPoint(id: String, coordinate: Coordinates) =
      LOCATION_OF_INTEREST.copy(
        id = id,
        geometry = Point(coordinate),
        surveyId = TEST_SURVEY.id,
        customId = "",
      )

    private fun createPolygon(id: String, coordinates: List<Coordinates>) =
      AREA_OF_INTEREST.copy(
        id = id,
        geometry = Polygon(LinearRing(coordinates)),
        surveyId = TEST_SURVEY.id,
        customId = "",
      )
  }
}
