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

import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.BaseHiltTest
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MapStateRepositoryTest : BaseHiltTest() {

  @Inject lateinit var mapStateRepository: MapStateRepository

  @Test
  fun testGetMapType_returnsSatellite() {
    assertThat(mapStateRepository.mapType).isEqualTo(GoogleMap.MAP_TYPE_HYBRID)
  }

  @Test
  fun testGetMapType_whenTerrain_returnsTerrain() {
    mapStateRepository.mapType = GoogleMap.MAP_TYPE_TERRAIN

    assertThat(mapStateRepository.mapType).isEqualTo(GoogleMap.MAP_TYPE_TERRAIN)
  }

  @Test
  fun testMapTypeFlowable_whenTerrain_returnsTerrain() {
    mapStateRepository.mapType = GoogleMap.MAP_TYPE_TERRAIN

    assertThat(mapStateRepository.mapTypeFlowable.blockingFirst())
      .isEqualTo(GoogleMap.MAP_TYPE_TERRAIN)
  }

  @Test
  fun testIsLocationLockEnabled_default() {
    assertThat(mapStateRepository.isLocationLockEnabled).isFalse()
  }

  @Test
  fun testIsLocationLockEnabled_whenLocked_returnsTrue() {
    mapStateRepository.isLocationLockEnabled = true

    assertThat(mapStateRepository.isLocationLockEnabled).isTrue()
  }
}
