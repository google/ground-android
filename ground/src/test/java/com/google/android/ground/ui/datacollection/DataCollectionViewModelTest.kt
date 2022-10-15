/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.datacollection

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalDataStoreModule
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.common.truth.Truth.assertThat
import com.sharedtest.getOrAwaitValue
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Provider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@UninstallModules(LocalDataStoreModule::class)
@RunWith(RobolectricTestRunner::class)
class DataCollectionViewModelTest : BaseHiltTest() {

  @get:Rule var instantExecutorRule = InstantTaskExecutorRule()

  @BindValue @Mock lateinit var localDataStore: LocalDataStore

  @BindValue @Mock lateinit var submissionRepository: SubmissionRepository

  lateinit var dataCollectionViewModel: DataCollectionViewModel

  @Inject lateinit var locationOfInterestHelper: LocationOfInterestHelper

  @Inject lateinit var popups: Provider<EphemeralPopups>

  @Before
  override fun setUp() {
    super.setUp()

    whenever(submissionRepository.createSubmission(any(), any(), any()))
      .doReturn(Single.just(DataCollectionTestData.submission))
    dataCollectionViewModel =
      DataCollectionViewModel(submissionRepository, locationOfInterestHelper, popups)
  }

  @Test
  fun testJobNameIsSetCorrectly() {
    dataCollectionViewModel.loadSubmissionDetails(DataCollectionTestData.args)

    assertThat(dataCollectionViewModel.jobName.getOrAwaitValue())
      .isEqualTo(DataCollectionTestData.jobName)
  }

  @Test
  fun testLoiNameIsSetCorrectly() {
    dataCollectionViewModel.loadSubmissionDetails(DataCollectionTestData.args)

    assertThat(dataCollectionViewModel.loiName.getOrAwaitValue())
      .isEqualTo(DataCollectionTestData.loiName)
  }
}
