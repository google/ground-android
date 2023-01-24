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
package com.google.android.ground.repository

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.coroutines.DefaultDispatcher
import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalDataStoreModule
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.room.RoomLocalDataStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Loadable.LoadState.ERROR
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.reactivex.Completable
import io.reactivex.Maybe
import java8.util.Optional
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@UninstallModules(LocalDataStoreModule::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveyRepositoryTest : BaseHiltTest() {
  @BindValue @InjectMocks var mockLocalDataStore: LocalDataStore = RoomLocalDataStore()

  @BindValue @Mock lateinit var mockLocalSurveyStore: LocalSurveyStore

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @Inject lateinit var surveyRepository: SurveyRepository

  @Inject lateinit var localValueStore: LocalValueStore

  @DefaultDispatcher @Inject lateinit var testDispatcher: CoroutineDispatcher

  @Before
  override fun setUp() {
    super.setUp()
    `when`(mockLocalSurveyStore.insertOrUpdateSurvey(any())).thenReturn(Completable.complete())
  }

  @Test
  fun activateSurvey_firstTime() =
    runTest(testDispatcher) {
      clearLocalTestSurvey()
      fakeRemoteDataStore.setTestSurvey(SURVEY)

      surveyRepository.activateSurvey(SURVEY.id)
      advanceUntilIdle()

      surveyRepository.activeSurvey.test().assertValue(Optional.of(SURVEY))
      surveyRepository.surveyLoadingState.test().assertValue(Loadable.loaded(SURVEY))
      verify(mockLocalSurveyStore).insertOrUpdateSurvey(SURVEY)
      assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isTrue()
    }

  @Test
  fun activateSurvey_firstTime_handleRemoteFailure() =
    runTest(testDispatcher) {
      clearLocalTestSurvey()
      fakeRemoteDataStore.failOnLoadSurvey = true

      surveyRepository.activateSurvey(SURVEY.id)
      advanceUntilIdle()

      surveyRepository.surveyLoadingState.test().assertValue { it.state == ERROR }
      verify(mockLocalSurveyStore, never()).insertOrUpdateSurvey(SURVEY)
      assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isFalse()
    }

  @Test
  fun activateSurvey_alreadyAvailableOffline() =
    runTest(testDispatcher) {
      setLocalTestSurvey(SURVEY)

      surveyRepository.activateSurvey(SURVEY.id)
      advanceUntilIdle()

      surveyRepository.activeSurvey.test().assertValue(Optional.of(SURVEY))
      surveyRepository.surveyLoadingState.test().assertValue(Loadable.loaded(SURVEY))
      verify(mockLocalSurveyStore, never()).insertOrUpdateSurvey(any())
      assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isFalse()
    }

  @Test
  fun loadLastActiveSurvey() =
    runTest(testDispatcher) {
      localValueStore.lastActiveSurveyId = SURVEY.id
      setLocalTestSurvey(SURVEY)

      surveyRepository.loadLastActiveSurvey()
      advanceUntilIdle()

      surveyRepository.activeSurvey.test().assertValue(Optional.of(SURVEY))
      surveyRepository.surveyLoadingState.test().assertValue(Loadable.loaded(SURVEY))
    }

  @Test
  fun loadLastActiveSurvey_noneSet() =
    runTest(testDispatcher) {
      localValueStore.lastActiveSurveyId = ""
      setLocalTestSurvey(SURVEY)

      surveyRepository.loadLastActiveSurvey()
      advanceUntilIdle()

      surveyRepository.activeSurvey.test().assertEmpty()
      surveyRepository.surveyLoadingState.test().assertEmpty()
    }

  private fun clearLocalTestSurvey() {
    `when`(mockLocalSurveyStore.getSurveyById(anyString())).thenReturn(Maybe.empty())
  }

  private fun setLocalTestSurvey(survey: Survey) {
    `when`(mockLocalSurveyStore.getSurveyById(anyString())).thenReturn(Maybe.just(survey))
  }
}
