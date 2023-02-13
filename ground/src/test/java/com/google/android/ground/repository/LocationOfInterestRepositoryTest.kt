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
import com.google.android.ground.model.mutation.Mutation.Type.CREATE
import com.google.android.ground.persistence.sync.MutationSyncWorkManager
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

  private val mutation = LOCATION_OF_INTEREST.toMutation(CREATE, USER.id)

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
  fun testApplyAndEnqueue_createsLocalLoi() {
    mockEnqueueSyncWorker()
    locationOfInterestRepository.applyAndEnqueue(mutation).test().assertNoErrors().assertComplete()

    locationOfInterestRepository
      .getOfflineLocationOfInterest(SURVEY.id, LOCATION_OF_INTEREST.id)
      .test()
      .assertNoErrors()
      .assertValue(LOCATION_OF_INTEREST)
  }

  @Test
  fun testApplyAndEnqueue_enqueuesLoiMutation() {
    mockEnqueueSyncWorker()

    locationOfInterestRepository.applyAndEnqueue(mutation).test().assertNoErrors().assertComplete()

    locationOfInterestRepository
      .getIncompleteLocationOfInterestMutationsOnceAndStream(LOCATION_OF_INTEREST.id)
      .test()
      .assertNoErrors()
      .assertValue(listOf(mutation.copy(id = 1)))
  }

  @Test
  fun testApplyAndEnqueue_enqueuesWorker() {
    mockEnqueueSyncWorker()

    locationOfInterestRepository.applyAndEnqueue(mutation).test().assertNoErrors().assertComplete()

    verify(mockWorkManager).enqueueSyncWorker(LOCATION_OF_INTEREST.id)
  }

  @Test
  fun testApplyAndEnqueue_returnsErrorOnWorkerSyncFailure() {
    `when`(mockWorkManager.enqueueSyncWorker(anyString())).thenReturn(Completable.error(Error()))

    locationOfInterestRepository
      .applyAndEnqueue(LOCATION_OF_INTEREST.toMutation(CREATE, USER.id))
      .test()
      .assertError(Error::class.java)
      .assertNotComplete()

    verify(mockWorkManager, times(1)).enqueueSyncWorker(LOCATION_OF_INTEREST.id)
  }

  // TODO(#1373): Add tests for new LOI sync once implemented (create, update, delete, error).

  // TODO(#1373): Add tests for getLocationsOfInterest once new LOI sync implemented.
}
