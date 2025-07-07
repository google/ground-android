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
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.mutation.Mutation.Type.CREATE
import org.groundplatform.android.persistence.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.persistence.remote.FakeRemoteDataStore
import org.groundplatform.android.persistence.sync.MutationSyncWorkManager
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.ui.map.Bounds
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocationOfInterestRepositoryTest : BaseHiltTest() {
  @BindValue @Mock lateinit var mockWorkManager: MutationSyncWorkManager

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var locationOfInterestRepository: LocationOfInterestRepository
  @Inject lateinit var localLoiStore: LocalLocationOfInterestStore
  @Inject lateinit var mutationRepository: MutationRepository
  @Inject lateinit var userRepository: UserRepository
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase

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
  fun testApplyAndEnqueue_createsLocalLoi() = runWithTestDispatcher {
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
  fun testApplyAndEnqueue_enqueuesLoiMutation() = runWithTestDispatcher {
    locationOfInterestRepository.applyAndEnqueue(mutation)

    mutationRepository.getSurveyMutationsFlow(TEST_SURVEY).test {
      assertThat(expectMostRecentItem()).isEqualTo(listOf(mutation.copy(id = 1)))
    }
  }

  @Test
  fun testApplyAndEnqueue_enqueuesWorker() = runWithTestDispatcher {
    locationOfInterestRepository.applyAndEnqueue(mutation)

    verify(mockWorkManager).enqueueSyncWorker()
  }

  @Test
  fun testApplyAndEnqueue_returnsErrorOnWorkerSyncFailure() = runWithTestDispatcher {
    `when`(mockWorkManager.enqueueSyncWorker()).thenThrow(Error())

    assertFailsWith<Error> {
      locationOfInterestRepository.applyAndEnqueue(
        LOCATION_OF_INTEREST.toMutation(CREATE, TEST_USER.id)
      )
    }

    verify(mockWorkManager, times(1)).enqueueSyncWorker()
  }

  // TODO: Add tests for new LOI sync once implemented (create, update, delete, error).
  // Issue URL: https://github.com/google/ground-android/issues/1373

  // TODO: Add tests for getLocationsOfInterest once new LOI sync implemented.
  // Issue URL: https://github.com/google/ground-android/issues/1373

  @Test
  fun testLoiWithinBounds_whenOutOfBounds_returnsEmptyList() = runWithTestDispatcher {
    val southwest = Coordinates(-60.0, -60.0)
    val northeast = Coordinates(-50.0, -50.0)

    locationOfInterestRepository.getWithinBounds(TEST_SURVEY, Bounds(southwest, northeast)).test {
      assertThat(expectMostRecentItem()).isEmpty()
    }
  }

  @Test
  fun testLoiWithinBounds_whenSomeLOIsInsideBounds_returnsPartialList() = runWithTestDispatcher {
    val southwest = Coordinates(-20.0, -20.0)
    val northeast = Coordinates(-10.0, -10.0)

    locationOfInterestRepository.getWithinBounds(TEST_SURVEY, Bounds(southwest, northeast)).test {
      assertThat(expectMostRecentItem())
        .isEqualTo(listOf(TEST_POINT_OF_INTEREST_1, TEST_AREA_OF_INTEREST_1))
    }
  }

  @Test
  fun testLoiWithinBounds_whenAllLOIsInsideBounds_returnsCompleteList() = runWithTestDispatcher {
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
