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

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.capture
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalDataStoreModule
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.error
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.loaded
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.modified
import com.google.android.ground.persistence.remote.RemoteDataEvent.Companion.removed
import com.google.android.ground.persistence.sync.DataSyncWorkManager
import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.*
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner

// TODO: Include a test for Polygon locationOfInterest
@HiltAndroidTest
@UninstallModules(LocalDataStoreModule::class)
@RunWith(RobolectricTestRunner::class)
class LocationOfInterestRepositoryTest : BaseHiltTest() {
  @BindValue @Mock lateinit var mockLocalDataStore: LocalDataStore
  @BindValue @Mock lateinit var mockSurveyRepository: SurveyRepository
  @BindValue @Mock lateinit var mockWorkManager: DataSyncWorkManager

  @Captor lateinit var captorLoiMutation: ArgumentCaptor<LocationOfInterestMutation>

  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var locationOfInterestRepository: LocationOfInterestRepository

  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setUser(FakeData.USER)
  }

  private fun mockApplyAndEnqueue() {
    Mockito.doReturn(Completable.complete())
      .`when`(mockLocalDataStore)
      .applyAndEnqueue(capture(captorLoiMutation))
  }

  private fun mockEnqueueSyncWorker() {
    Mockito.`when`(mockWorkManager.enqueueSyncWorker(ArgumentMatchers.anyString()))
      .thenReturn(Completable.complete())
  }

  @Test
  fun testApplyAndEnqueue() {
    mockApplyAndEnqueue()
    mockEnqueueSyncWorker()
    locationOfInterestRepository
      .applyAndEnqueue(
        FakeData.LOCATION_OF_INTEREST.toMutation(Mutation.Type.CREATE, FakeData.USER.id)
      )
      .test()
      .assertNoErrors()
      .assertComplete()
    val (_, type, syncStatus, _, locationOfInterestId) = captorLoiMutation.value
    assertThat(type).isEqualTo(Mutation.Type.CREATE)
    assertThat(syncStatus).isEqualTo(SyncStatus.PENDING)
    assertThat(locationOfInterestId).isEqualTo(FakeData.LOCATION_OF_INTEREST.id)
    Mockito.verify(mockLocalDataStore, Mockito.times(1))
      .applyAndEnqueue(any<LocationOfInterestMutation>())
    Mockito.verify(mockWorkManager, Mockito.times(1))
      .enqueueSyncWorker(FakeData.LOCATION_OF_INTEREST.id)
  }

  @Test
  fun testApplyAndEnqueue_returnsError() {
    mockEnqueueSyncWorker()
    Mockito.doReturn(Completable.error(NullPointerException()))
      .`when`(mockLocalDataStore)
      .applyAndEnqueue(any<LocationOfInterestMutation>())
    locationOfInterestRepository
      .applyAndEnqueue(
        FakeData.LOCATION_OF_INTEREST.toMutation(Mutation.Type.CREATE, FakeData.USER.id)
      )
      .test()
      .assertError(NullPointerException::class.java)
      .assertNotComplete()
    Mockito.verify(mockLocalDataStore, Mockito.times(1))
      .applyAndEnqueue(any<LocationOfInterestMutation>())
    Mockito.verify(mockWorkManager, Mockito.times(1))
      .enqueueSyncWorker(FakeData.LOCATION_OF_INTEREST.id)
  }

  @Test
  fun testEnqueueSyncWorker_returnsError() {
    mockApplyAndEnqueue()
    Mockito.`when`(mockWorkManager.enqueueSyncWorker(ArgumentMatchers.anyString()))
      .thenReturn(Completable.error(NullPointerException()))
    locationOfInterestRepository
      .applyAndEnqueue(
        FakeData.LOCATION_OF_INTEREST.toMutation(Mutation.Type.CREATE, FakeData.USER.id)
      )
      .test()
      .assertError(NullPointerException::class.java)
      .assertNotComplete()

    Mockito.verify(mockLocalDataStore, Mockito.times(1))
      .applyAndEnqueue(any<LocationOfInterestMutation>())
    Mockito.verify(mockWorkManager, Mockito.times(1))
      .enqueueSyncWorker(FakeData.LOCATION_OF_INTEREST.id)
  }

  @Test
  fun testSyncLocationsOfInterest_loaded() {
    fakeRemoteDataStore.streamLoiOnce(loaded("entityId", FakeData.LOCATION_OF_INTEREST))
    Mockito.`when`(mockLocalDataStore.mergeLocationOfInterest(FakeData.LOCATION_OF_INTEREST))
      .thenReturn(Completable.complete())
    locationOfInterestRepository
      .syncLocationsOfInterest(FakeData.SURVEY)
      .test()
      .assertNoErrors()
      .assertComplete()
    Mockito.verify(mockLocalDataStore, Mockito.times(1))
      .mergeLocationOfInterest(FakeData.LOCATION_OF_INTEREST)
  }

  @Test
  fun testSyncLocationsOfInterest_modified() {
    fakeRemoteDataStore.streamLoiOnce(modified("entityId", FakeData.LOCATION_OF_INTEREST))
    Mockito.`when`(mockLocalDataStore.mergeLocationOfInterest(FakeData.LOCATION_OF_INTEREST))
      .thenReturn(Completable.complete())
    locationOfInterestRepository
      .syncLocationsOfInterest(FakeData.SURVEY)
      .test()
      .assertNoErrors()
      .assertComplete()
    Mockito.verify(mockLocalDataStore, Mockito.times(1))
      .mergeLocationOfInterest(FakeData.LOCATION_OF_INTEREST)
  }

  @Test
  fun testSyncLocationsOfInterest_removed() {
    fakeRemoteDataStore.streamLoiOnce(removed("entityId"))
    Mockito.`when`(mockLocalDataStore.deleteLocationOfInterest(ArgumentMatchers.anyString()))
      .thenReturn(Completable.complete())
    locationOfInterestRepository.syncLocationsOfInterest(FakeData.SURVEY).test().assertComplete()
    Mockito.verify(mockLocalDataStore, Mockito.times(1)).deleteLocationOfInterest("entityId")
  }

  @Test
  fun testSyncLocationsOfInterest_error() {
    fakeRemoteDataStore.streamLoiOnce(error(Throwable("Foo error")))
    locationOfInterestRepository
      .syncLocationsOfInterest(FakeData.SURVEY)
      .test()
      .assertNoErrors()
      .assertComplete()
  }

  @Test
  fun testGetLocationsOfInterestOnceAndStream() {
    Mockito.`when`(mockLocalDataStore.getLocationsOfInterestOnceAndStream(FakeData.SURVEY))
      .thenReturn(Flowable.just(ImmutableSet.of(FakeData.LOCATION_OF_INTEREST)))
    locationOfInterestRepository
      .getLocationsOfInterestOnceAndStream(FakeData.SURVEY)
      .test()
      .assertValue(ImmutableSet.of(FakeData.LOCATION_OF_INTEREST))
  }

  @Test
  fun testGetLocationsOfInterest_surveyNotPresent() {
    Mockito.`when`(mockSurveyRepository.getSurvey(ArgumentMatchers.anyString()))
      .thenReturn(Single.error(NoSuchElementException()))
    locationOfInterestRepository
      .getLocationOfInterest("non_existent_survey_id", "loi_id")
      .test()
      .assertFailure(NoSuchElementException::class.java)
  }

  @Test
  fun testGetLocationsOfInterest_surveyPresent() {
    Mockito.`when`(mockSurveyRepository.getSurvey(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(FakeData.SURVEY))
    Mockito.`when`(
        mockLocalDataStore.getLocationOfInterest(FakeData.SURVEY, FakeData.LOCATION_OF_INTEREST.id)
      )
      .thenReturn(Maybe.just(FakeData.LOCATION_OF_INTEREST))
    locationOfInterestRepository
      .getLocationOfInterest(FakeData.SURVEY.id, FakeData.LOCATION_OF_INTEREST.id)
      .test()
      .assertResult(FakeData.LOCATION_OF_INTEREST)
    locationOfInterestRepository
      .getLocationOfInterest(
        FakeData.LOCATION_OF_INTEREST.toMutation(Mutation.Type.UPDATE, "user_id")
      )
      .test()
      .assertResult(FakeData.LOCATION_OF_INTEREST)
  }

  @Test
  fun testGetLocationOfInterest_whenLocationOfInterestIsNotPresent() {
    Mockito.`when`(mockSurveyRepository.getSurvey(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(FakeData.SURVEY))
    Mockito.`when`(
        mockLocalDataStore.getLocationOfInterest(FakeData.SURVEY, FakeData.LOCATION_OF_INTEREST.id)
      )
      .thenReturn(Maybe.empty())
    locationOfInterestRepository
      .getLocationOfInterest(FakeData.SURVEY.id, FakeData.LOCATION_OF_INTEREST.id)
      .test()
      .assertFailureAndMessage(
        NotFoundException::class.java,
        "Location of interest not found loi id"
      )
  }

  @Test
  fun testNewLocationOfInterest() {
    val testDate = Date()
    val (id, _, _, surveyId, locationOfInterestId, userId, clientTimestamp, _, _, jobId, location) =
      locationOfInterestRepository.newMutation(
        "foo_survey_id",
        "foo_job_id",
        FakeData.POINT,
        testDate
      )
    assertThat(id).isNull()
    assertThat(locationOfInterestId).isEqualTo("TEST UUID")
    assertThat(surveyId).isEqualTo("foo_survey_id")
    assertThat(jobId).isEqualTo("foo_job_id")
    assertThat(location).isEqualTo(FakeData.POINT)
    assertThat(userId).isEqualTo(FakeData.USER.id)
    assertThat(clientTimestamp).isEqualTo(testDate)
  }

  @Test
  fun testNewPolygonOfInterest() {
    val testDate = Date()
    val (id, _, _, surveyId, locationOfInterestId, userId, clientTimestamp, _, _, jobId, polygon) =
      locationOfInterestRepository.newPolygonOfInterestMutation(
        "foo_survey_id",
        "foo_job_id",
        FakeData.VERTICES,
        testDate
      )
    assertThat(id).isNull()
    assertThat(locationOfInterestId).isEqualTo("TEST UUID")
    assertThat(surveyId).isEqualTo("foo_survey_id")
    assertThat(jobId).isEqualTo("foo_job_id")
    assertThat(polygon?.vertices).isEqualTo(FakeData.VERTICES)
    assertThat(userId).isEqualTo(FakeData.USER.id)
    assertThat(clientTimestamp).isEqualTo(testDate)
  }

  @Test
  fun testGetIncompleteLocationOfInterestMutationsOnceAndStream() {
    locationOfInterestRepository.getIncompleteLocationOfInterestMutationsOnceAndStream("loi_id_1")
    Mockito.verify(mockLocalDataStore, Mockito.times(1))
      .getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
        "loi_id_1",
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS,
        MutationEntitySyncStatus.FAILED
      )
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
