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

import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.data.FakeLocalValueStore
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.CameraPosition
import org.groundplatform.domain.model.map.MapType

class MapStateRepositoryTest {
  private val fakeLocalValueStore = FakeLocalValueStore()
  private val mapStateRepository = MapStateRepository(fakeLocalValueStore)

  private val surveyId = "survey_1"
  private val coordinates = Coordinates(10.0, 20.0)

  @Test
  fun getMapType_defaultIsTerrain() {
    assertEquals(MapType.TERRAIN, mapStateRepository.mapType)
  }

  @Test
  fun getMapType_whenSet_returnsUpdatedValue() {
    mapStateRepository.mapType = MapType.SATELLITE
    assertEquals(MapType.SATELLITE, mapStateRepository.mapType)
  }

  @Test
  fun mapTypeFlow_emitsUpdatedValue() = runTest {
    mapStateRepository.mapType = MapType.SATELLITE

    mapStateRepository.mapTypeFlow.test { assertEquals(MapType.SATELLITE, expectMostRecentItem()) }
  }

  @Test
  fun isOfflineImageryEnabled_defaultIsTrue() {
    assertTrue(mapStateRepository.isOfflineImageryEnabled)
  }

  @Test
  fun isOfflineImageryEnabled_whenSet_updatesFlow() = runTest {
    mapStateRepository.isOfflineImageryEnabled = false

    mapStateRepository.offlineImageryEnabledFlow.test { assertFalse(expectMostRecentItem()) }
  }

  @Test
  fun isLocationLockEnabled_toggle() {
    assertFalse(mapStateRepository.isLocationLockEnabled)

    mapStateRepository.isLocationLockEnabled = true
    assertTrue(mapStateRepository.isLocationLockEnabled)
  }

  @Test
  fun cameraPosition_setAndGet() {
    fakeLocalValueStore.lastActiveSurveyId = surveyId
    val position = CameraPosition(coordinates = coordinates)

    mapStateRepository.setCameraPosition(position)
    assertEquals(position, mapStateRepository.getCameraPosition(surveyId))
  }

  @Test
  fun clearCameraPosition_removesValue() {
    fakeLocalValueStore.lastActiveSurveyId = surveyId
    val position = CameraPosition(coordinates = coordinates)

    mapStateRepository.setCameraPosition(position)
    mapStateRepository.clearCameraPosition(surveyId)
    assertNull(mapStateRepository.getCameraPosition(surveyId))
  }
}
