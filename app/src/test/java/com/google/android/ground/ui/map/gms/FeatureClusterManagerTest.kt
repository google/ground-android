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
package com.google.android.ground.ui.map.gms

import android.os.Looper.getMainLooper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData
import com.google.android.ground.ui.map.gms.features.FeatureClusterManager
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.collections.MarkerManager
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class FeatureClusterManagerTest : BaseHiltTest() {
  @Mock private lateinit var map: GoogleMap

  private lateinit var featureClusterManager: FeatureClusterManager

  @Before
  override fun setUp() {
    super.setUp()
    featureClusterManager =
      FeatureClusterManager(
        ApplicationProvider.getApplicationContext(),
        map,
        object : MarkerManager(map) {},
      )
    shadowOf(getMainLooper()).idle()
  }

  @Test
  fun `addFeature() adds clusterable features to cluster manager`() {
    featureClusterManager.addFeature(FakeData.LOCATION_OF_INTEREST_FEATURE)
    assertThat(featureClusterManager.algorithm.items)
      .contains(FakeData.LOCATION_OF_INTEREST_CLUSTER_ITEM)
  }

  @Test
  fun `removeFeature() removes existing`() {
    featureClusterManager.addFeature(
      FakeData.LOCATION_OF_INTEREST_FEATURE.copy(tag = FakeData.LOCATION_OF_INTEREST_FEATURE.tag)
    )
    featureClusterManager.removeFeature(FakeData.LOCATION_OF_INTEREST_FEATURE.tag)

    assertThat(featureClusterManager.algorithm.items).isEmpty()
  }
}
