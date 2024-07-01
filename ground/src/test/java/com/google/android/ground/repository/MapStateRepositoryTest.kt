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

import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapType
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MapStateRepositoryTest : BaseHiltTest() {

  @Inject lateinit var mapStateRepository: MapStateRepository
  @Inject lateinit var localValueStore: LocalValueStore

  @Test
  fun getMapType_whenNotSet_returnsDefault() {
    assertThat(mapStateRepository.mapType).isEqualTo(MapType.DEFAULT)
  }

  @Test
  fun getMapType_whenTerrain_returnsTerrain() {
    mapStateRepository.mapType = MapType.TERRAIN

    assertThat(mapStateRepository.mapType).isEqualTo(MapType.TERRAIN)
  }

  @Test
  fun mapTypeFlowable_whenTerrain_returnsTerrain() = runWithTestDispatcher {
    mapStateRepository.mapType = MapType.TERRAIN

    mapStateRepository.mapTypeFlow.test {
      assertThat(expectMostRecentItem()).isEqualTo(MapType.TERRAIN)
    }
  }

  @Test
  fun isOfflineImageryEnabled_default() = runWithTestDispatcher {
    assertThat(mapStateRepository.isOfflineImageryEnabled).isTrue()
  }

  @Test
  fun isOfflineImageryEnabled_whenEnabled_returnsTrue() = runWithTestDispatcher {
    mapStateRepository.isOfflineImageryEnabled = true

    mapStateRepository.offlineImageryEnabledFlow.test {
      assertThat(expectMostRecentItem()).isTrue()
    }
  }

  @Test
  fun isLocationEnabled_whenNotSet_returnsDefault() {
    assertThat(mapStateRepository.isLocationLockEnabled).isEqualTo(false)

    mapStateRepository.isLocationLockEnabled = true
    assertThat(mapStateRepository.isLocationLockEnabled).isEqualTo(true)
  }

  @Test
  fun cameraPosition_whenSet_returnsCameraPosition() {
    localValueStore.lastActiveSurveyId = SURVEY_ID
    mapStateRepository.setCameraPosition(CameraPosition(target = TARGET))

    assertThat(mapStateRepository.getCameraPosition(SURVEY_ID))
      .isEqualTo(CameraPosition(target = TARGET))
  }

  companion object {
    private val TARGET = FakeData.COORDINATES
    private const val SURVEY_ID = "survey_id"
  }

}
