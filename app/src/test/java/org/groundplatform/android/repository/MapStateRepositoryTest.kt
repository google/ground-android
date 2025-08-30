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
package org.groundplatform.android.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.model.map.MapType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MapStateRepositoryTest : BaseHiltTest() {

  @Inject lateinit var mapStateRepository: MapStateRepository
  @Inject lateinit var localValueStore: LocalValueStore

  @Test
  fun `getMapType() should return terrain value when not set`() {
    assertThat(mapStateRepository.mapType).isEqualTo(MapType.TERRAIN)
  }

  @Test
  fun `getMapType() should return terrain value when set to terrain`() {
    mapStateRepository.mapType = MapType.TERRAIN

    assertThat(mapStateRepository.mapType).isEqualTo(MapType.TERRAIN)
  }

  @Test
  fun `mapTypeFlow should have value terrain when set to terrain`() = runWithTestDispatcher {
    mapStateRepository.mapType = MapType.TERRAIN

    mapStateRepository.mapTypeFlow.test {
      assertThat(expectMostRecentItem()).isEqualTo(MapType.TERRAIN)
    }
  }

  @Test
  fun `isOfflineImageryEnabled have value true by default`() = runWithTestDispatcher {
    assertThat(mapStateRepository.isOfflineImageryEnabled).isTrue()
  }

  @Test
  fun `isOfflineImageryEnabled have true when enabled`() = runWithTestDispatcher {
    mapStateRepository.isOfflineImageryEnabled = true

    mapStateRepository.offlineImageryEnabledFlow.test {
      assertThat(expectMostRecentItem()).isTrue()
    }
  }

  @Test
  fun `isLocationLockEnabled is false by default`() {
    assertThat(mapStateRepository.isLocationLockEnabled).isEqualTo(false)
  }

  @Test
  fun `isLocationLockEnabled is true when set to true`() {
    mapStateRepository.isLocationLockEnabled = true
    assertThat(mapStateRepository.isLocationLockEnabled).isEqualTo(true)
  }

  @Test
  fun `getCameraPosition() should return same value as passed to setCameraPosition()`() {
    localValueStore.lastActiveSurveyId = SURVEY_ID
    mapStateRepository.setCameraPosition(CameraPosition(coordinates = COORDINATES))

    assertThat(mapStateRepository.getCameraPosition(SURVEY_ID))
      .isEqualTo(CameraPosition(coordinates = COORDINATES))
  }

  companion object {
    private val COORDINATES = FakeData.COORDINATES
    private const val SURVEY_ID = "survey_id"
  }
}
