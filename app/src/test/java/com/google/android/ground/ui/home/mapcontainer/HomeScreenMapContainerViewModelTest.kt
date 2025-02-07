/*
 * Copyright 2025 Google LLC
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

package com.google.android.ground.ui.home.mapcontainer

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData.ADHOC_JOB
import com.google.android.ground.FakeData.LOCATION_OF_INTEREST
import com.google.android.ground.FakeData.LOCATION_OF_INTEREST_FEATURE
import com.google.android.ground.FakeData.SURVEY
import com.google.android.ground.FakeData.USER
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.persistence.remote.FakeRemoteDataStore
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.FakeAuthenticationManager
import com.google.android.ground.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import com.google.android.ground.ui.home.mapcontainer.jobs.SelectedLoiSheetData
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.CameraPosition
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenMapContainerViewModelTest : BaseHiltTest() {
  @Inject lateinit var viewModel: HomeScreenMapContainerViewModel
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var authenticationManager: FakeAuthenticationManager
  @Inject lateinit var remoteDataStore: FakeRemoteDataStore
  @Inject lateinit var userRepository: UserRepository
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @BindValue @Mock lateinit var loiRepository: LocationOfInterestRepository

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  override fun setUp() {
    super.setUp()
    runWithTestDispatcher {
      // Setup user
      authenticationManager.setUser(USER)
      userRepository.saveUserDetails(USER)

      // Setup survey and LOIs
      remoteDataStore.surveys = listOf(SURVEY)
      remoteDataStore.predefinedLois = listOf(LOCATION_OF_INTEREST)
      activateSurvey(SURVEY.id)
      advanceUntilIdle()
      `when`(loiRepository.getWithinBounds(SURVEY, BOUNDS))
        .thenReturn(flowOf(listOf(LOCATION_OF_INTEREST)))
      viewModel.onMapCameraMoved(CAMERA_POSITION)
      advanceUntilIdle()
    }
  }

  @Test
  fun `renders the job card when zoomed into LOI and clicked on`() = runWithTestDispatcher {
    viewModel.onFeatureClicked(features = setOf(LOCATION_OF_INTEREST_FEATURE))
    val pair = viewModel.processDataCollectionEntryPoints().first()
    assertThat(pair.first).isEqualTo(SelectedLoiSheetData(LOCATION_OF_INTEREST))
    assertThat(pair.second).isEqualTo(listOf(AdHocDataCollectionButtonData(ADHOC_JOB)))
  }

  companion object {
    private val BOUNDS = Bounds(Coordinates(-20.0, -20.0), Coordinates(-10.0, -10.0))
    val CAMERA_POSITION =
      CameraPosition(
        coordinates = LOCATION_OF_INTEREST.geometry.center(),
        zoomLevel = 16f,
        bounds = BOUNDS,
      )
  }
}
