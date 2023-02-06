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
package com.google.android.ground.repository

import android.content.res.Resources.NotFoundException
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.loaded
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.modified
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.removed
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.LOCATION_OF_INTEREST
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.FakeData.USER
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

// TODO: Include a test for Polygon locationOfInterest
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocationOfInterestRepositoryTest : BaseHiltTest() {
  @BindValue @Mock lateinit var mockWorkManager: MutationSyncWorkManager

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var locationOfInterestRepository: LocationOfInterestRepository
  @Inject lateinit var userRepository: UserRepository
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var testDispatcher: TestDispatcher

  @Before
  override fun setUp() {
    super.setUp()
    runTest(testDispatcher) {
      userRepository.saveUser(USER).await()
      fakeAuthenticationManager.setUser(USER)
      fakeRemoteDataStore.surveys = listOf(SURVEY)
      surveyRepository.syncSurveyWithRemote(SURVEY.id).await()
      advanceUntilIdle()
    }
  }

  private fun mockEnqueueSyncWorker() {
    `when`(mockWorkManager.enqueueSyncWorker(anyString())).thenReturn(Completable.complete())
  }

  @Test
  fun testApplyAndEnqueue() {
    mockEnqueueSyncWorker()
    val mutation = LOCATION_OF_INTEREST.toMutation(Mutation.Type.CREATE, USER.id)
    locationOfInterestRepository.applyAndEnqueue(mutation).test().assertNoErrors().assertComplete()

    locationOfInterestRepository
      .getOfflineLocationOfInterest(SURVEY.id, LOCATION_OF_INTEREST.id)
      .test()
      .assertValue(LOCATION_OF_INTEREST)
    locationOfInterestRepository
      .getIncompleteLocationOfInterestMutationsOnceAndStream(LOCATION_OF_INTEREST.id)
      .test()
      .assertValue(listOf(mutation))
    verify(mockWorkManager, times(1)).enqueueSyncWorker(LOCATION_OF_INTEREST.id)
  }

  @Test
  fun testApplyAndEnqueue_returnsError() {
    mockEnqueueSyncWorker()
    //    doReturn(Completable.error(NullPointerException()))
    //      .`when`(mockLocalLocationOfInterestStore)
    //      .applyAndEnqueue(any())
    locationOfInterestRepository
      .applyAndEnqueue(LOCATION_OF_INTEREST.toMutation(Mutation.Type.CREATE, USER.id))
      .test()
      .assertError(NullPointerException::class.java)
      .assertNotComplete()
    //    verify(mockLocalLocationOfInterestStore, Mockito.times(1)).applyAndEnqueue(any())
    verify(mockWorkManager, times(1)).enqueueSyncWorker(LOCATION_OF_INTEREST.id)
  }

  @Test
  fun testEnqueueSyncWorker_returnsError() {
    //    mockApplyAndEnqueue()
    `when`(mockWorkManager.enqueueSyncWorker(anyString()))
      .thenReturn(Completable.error(NullPointerException()))
    locationOfInterestRepository
      .applyAndEnqueue(LOCATION_OF_INTEREST.toMutation(Mutation.Type.CREATE, USER.id))
      .test()
      .assertError(NullPointerException::class.java)
      .assertNotComplete()

    //    verify(mockLocalLocationOfInterestStore, times(1)).applyAndEnqueue(any())
    verify(mockWorkManager, times(1)).enqueueSyncWorker(LOCATION_OF_INTEREST.id)
  }

  @Test
  fun testSyncLocationsOfInterest_loaded() {
    fakeRemoteDataStore.streamLoiOnce(loaded("entityId", LOCATION_OF_INTEREST))
    //    `when`(mockLocalLocationOfInterestStore.merge(LOCATION_OF_INTEREST))
    //      .thenReturn(Completable.complete())
    locationOfInterestRepository
      .syncLocationsOfInterest(SURVEY)
      .test()
      .assertNoErrors()
      .assertComplete()
    //    verify(mockLocalLocationOfInterestStore, Mockito.times(1)).merge(LOCATION_OF_INTEREST)
  }

  @Test
  fun testSyncLocationsOfInterest_modified() {
    fakeRemoteDataStore.streamLoiOnce(modified("entityId", LOCATION_OF_INTEREST))
    //    `when`(mockLocalLocationOfInterestStore.merge(LOCATION_OF_INTEREST))
    //      .thenReturn(Completable.complete())
    locationOfInterestRepository
      .syncLocationsOfInterest(SURVEY)
      .test()
      .assertNoErrors()
      .assertComplete()
    //    verify(mockLocalLocationOfInterestStore, Mockito.times(1)).merge(LOCATION_OF_INTEREST)
  }

  @Test
  fun testSyncLocationsOfInterest_removed() {
    fakeRemoteDataStore.streamLoiOnce(removed("entityId"))
    //    `when`(mockLocalLocationOfInterestStore.deleteLocationOfInterest(anyString()))
    //      .thenReturn(Completable.complete())

    locationOfInterestRepository.syncLocationsOfInterest(SURVEY).test().assertComplete()
    //    verify(mockLocalLocationOfInterestStore,
    // Mockito.times(1)).deleteLocationOfInterest("entityId")
  }

  @Test
  fun testSyncLocationsOfInterest_error() {
    fakeRemoteDataStore.streamLoiOnce(error(Throwable("Foo error")))
    locationOfInterestRepository
      .syncLocationsOfInterest(SURVEY)
      .test()
      .assertNoErrors()
      .assertComplete()
  }

  @Test
  fun testGetLocationsOfInterestOnceAndStream() {
    //
    // `when`(mockLocalLocationOfInterestStore.getLocationsOfInterestOnceAndStream(SURVEY))
    //      .thenReturn(Flowable.just(setOf(LOCATION_OF_INTEREST)))
    locationOfInterestRepository
      .getLocationsOfInterestOnceAndStream(SURVEY)
      .test()
      .assertValue(setOf(LOCATION_OF_INTEREST))
  }

  @Test
  fun testGetLocationsOfInterest_surveyNotPresent() {
    //    `when`(mockSurveyRepository.getOfflineSurvey(anyString()))
    //      .thenReturn(Single.error(NoSuchElementException()))
    locationOfInterestRepository
      .getOfflineLocationOfInterest("non_existent_survey_id", "loi_id")
      .test()
      .assertFailure(NoSuchElementException::class.java)
  }

  @Test
  fun testGetLocationsOfInterest_surveyPresent() {
    //    `when`(mockSurveyRepository.getOfflineSurvey(anyString()))
    //      .thenReturn(Single.just(SURVEY))
    //    `when`(
    //        mockLocalLocationOfInterestStore.getLocationOfInterest(
    //          SURVEY,
    //          LOCATION_OF_INTEREST.id
    //        )
    //      )
    //      .thenReturn(Maybe.just(LOCATION_OF_INTEREST))
    locationOfInterestRepository
      .getOfflineLocationOfInterest(SURVEY.id, LOCATION_OF_INTEREST.id)
      .test()
      .assertResult(LOCATION_OF_INTEREST)
  }

  @Test
  fun testGetLocationOfInterest_whenLocationOfInterestIsNotPresent() {
    //    `when`(mockSurveyRepository.getOfflineSurvey(anyString()))
    //      .thenReturn(Single.just(SURVEY))
    //    `when`(
    //        mockLocalLocationOfInterestStore.getLocationOfInterest(
    //          SURVEY,
    //          LOCATION_OF_INTEREST.id
    //        )
    //      )
    //      .thenReturn(Maybe.empty())
    locationOfInterestRepository
      .getOfflineLocationOfInterest(SURVEY.id, LOCATION_OF_INTEREST.id)
      .test()
      .assertFailureAndMessage(
        NotFoundException::class.java,
        "Location of interest not found loi id"
      )
  }

  @Test
  fun testGetIncompleteLocationOfInterestMutationsOnceAndStream() {

    locationOfInterestRepository.getIncompleteLocationOfInterestMutationsOnceAndStream("loi_id_1")
    //    verify(mockLocalLocationOfInterestStore, Mockito.times(1))
    //      .getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
    //        "loi_id_1",
    //        MutationEntitySyncStatus.PENDING,
    //        MutationEntitySyncStatus.IN_PROGRESS,
    //        MutationEntitySyncStatus.FAILED
    //      )
  }

  @Test
  fun testPolygonInfoDialogShown() {
    assertThat(locationOfInterestRepository.isPolygonInfoDialogShown).isFalse()

    locationOfInterestRepository.setPolygonDialogInfoShown(true)
    assertThat(locationOfInterestRepository.isPolygonInfoDialogShown).isTrue()

    locationOfInterestRepository.setPolygonDialogInfoShown(false)
    assertThat(locationOfInterestRepository.isPolygonInfoDialogShown).isFalse()
  }
}
