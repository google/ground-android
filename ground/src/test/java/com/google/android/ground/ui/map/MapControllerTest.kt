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
package com.google.android.ground.ui.map

import android.location.Location
import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.LocationManager
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MapControllerTest : BaseHiltTest() {
  @Mock lateinit var locationManager: LocationManager
  @Mock lateinit var locationOfInterestRepository: LocationOfInterestRepository
  @Mock lateinit var mapStateRepository: MapStateRepository

  @Inject lateinit var dispatcher: TestDispatcher
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope
  @Inject lateinit var surveyRepository: SurveyRepository

  private val locationSharedFlow: MutableSharedFlow<Location> = MutableSharedFlow()

  private lateinit var mapController: MapController

  @Before
  override fun setUp() {
    super.setUp()
    mapController =
      MapController(
        locationManager,
        locationOfInterestRepository,
        surveyRepository,
        mapStateRepository,
        externalScope
      )
    `when`(locationManager.locationUpdates).thenReturn(locationSharedFlow)
  }

  @Test
  fun testGetCameraUpdates_returnsNothing() = runWithTestDispatcher {
    mapController.getCameraUpdates().test { expectNoEvents() }
  }

  @Test
  fun testGetCameraUpdates_whenPanAndZoomCamera() = runWithTestDispatcher {
    mapController.panAndZoomCamera(TEST_COORDINATE)

    mapController.getCameraUpdates().test {
      assertThat(expectMostRecentItem()).isEqualTo(CameraPosition(TEST_COORDINATE, 18.0f))
    }
  }

  @Test
  fun testGetCameraUpdates_whenLocationUpdates() = runWithTestDispatcher {
    locationSharedFlow.emit(
      Location("test provider").apply {
        latitude = TEST_COORDINATE.lat
        longitude = TEST_COORDINATE.lng
      }
    )

    advanceUntilIdle()

    mapController.getCameraUpdates().test {
      assertThat(expectMostRecentItem()).isEqualTo(CameraPosition(TEST_COORDINATE, 18.0f))
    }
  }

  @Test
  fun testGetCameraUpdates_whenSurveyChanges_whenLastLocationNotAvailableAndNoLois_returnsNothing() =
    runWithTestDispatcher {
      `when`(mapStateRepository.getCameraPosition(any())).thenReturn(null)

      mapController.getCameraUpdates().test { expectNoEvents() }
    }

  @Ignore("MapController returns a result when debugger is attached, otherwise not. Fix this!")
  @Test
  fun testGetCameraUpdates_whenSurveyChanges_whenLastLocationNotAvailableAndHasLois_returnsNothing() =
    runWithTestDispatcher {
      surveyRepository.activeSurvey = TEST_SURVEY
      `when`(mapStateRepository.getCameraPosition(any())).thenReturn(null)
      `when`(locationOfInterestRepository.getAllGeometries(any()))
        .thenReturn(
          listOf(
            Point(Coordinates(10.0, 10.0)),
            Point(Coordinates(20.0, 20.0)),
            Point(Coordinates(30.0, 30.0))
          )
        )

      advanceUntilIdle()

      mapController.getCameraUpdates().test {
        assertThat(expectMostRecentItem()).isEqualTo(CameraPosition(TEST_COORDINATE, 18.0f))
      }
    }

  @Test
  fun testGetCameraUpdates_whenSurveyChanges_whenLastLocationAvailable() = runWithTestDispatcher {
    surveyRepository.activeSurvey = TEST_SURVEY
    `when`(mapStateRepository.getCameraPosition(any())).thenReturn(TEST_POSITION)

    mapController.getCameraUpdates().test {
      assertThat(expectMostRecentItem()).isEqualTo(TEST_POSITION.copy(isAllowZoomOut = true))
    }
  }

  companion object {
    private val TEST_COORDINATE = Coordinates(20.0, 30.0)
    private val TEST_POSITION = CameraPosition(TEST_COORDINATE)
    private val TEST_SURVEY = FakeData.SURVEY
  }
}
