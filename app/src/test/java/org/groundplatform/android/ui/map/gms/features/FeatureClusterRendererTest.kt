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
package org.groundplatform.android.ui.map.gms.features

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST_CLUSTER_ITEM
import org.groundplatform.android.common.Constants.CLUSTERING_ZOOM_THRESHOLD
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class FeatureClusterRendererTest : BaseHiltTest() {

  @Mock private lateinit var map: GoogleMap
  @Mock private lateinit var clusterManager: ClusterManager<FeatureClusterItem>
  @Mock private lateinit var cluster: Cluster<FeatureClusterItem>

  private lateinit var context: Context
  private lateinit var featureClusterRenderer: FeatureClusterRenderer

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
    com.google.android.gms.maps.MapsInitializer.initialize(context)
    featureClusterRenderer = FeatureClusterRenderer(context, map, clusterManager, 10f)
  }

  @Test
  fun `Should render as a cluster if the zoom is below the threshold`() {
    featureClusterRenderer.setZoom(CLUSTERING_ZOOM_THRESHOLD - 1f)
    assertTrue(featureClusterRenderer.invoke("shouldRenderAsCluster", cluster))
  }

  @Test
  fun `Should not render as a cluster when the zoom is above the threshold`() {
    featureClusterRenderer.setZoom(CLUSTERING_ZOOM_THRESHOLD + 1f)
    assertFalse(featureClusterRenderer.invoke("shouldRenderAsCluster", cluster))
  }

  @Test
  fun `Should force render if the zoom threshold was crossed from below to above`() {
    featureClusterRenderer.setZoom(CLUSTERING_ZOOM_THRESHOLD - 1f)
    featureClusterRenderer.setZoom(CLUSTERING_ZOOM_THRESHOLD + 1f)

    assertTrue(
      featureClusterRenderer.invoke(
        "shouldRender",
        emptySet<Cluster<FeatureClusterItem>>(),
        emptySet<Cluster<FeatureClusterItem>>(),
      )
    )
  }

  @Test
  fun `Should force render if the zoom threshold was crossed from above to below`() {
    featureClusterRenderer.setZoom(CLUSTERING_ZOOM_THRESHOLD + 1f)
    featureClusterRenderer.setZoom(CLUSTERING_ZOOM_THRESHOLD - 1f)

    assertTrue(
      featureClusterRenderer.invoke(
        "shouldRender",
        emptySet<Cluster<FeatureClusterItem>>(),
        emptySet<Cluster<FeatureClusterItem>>(),
      )
    )
  }

  @Test
  fun `Should not force render if the clusters haven't changed and the zoom doesn't cross the threshold`() {
    featureClusterRenderer.setZoom(10f)
    featureClusterRenderer.setZoom(11f)

    // Assuming clusters are same (empty vs empty), super implementation would return false
    assertFalse(
      featureClusterRenderer.invoke(
        "shouldRender",
        emptySet<Cluster<FeatureClusterItem>>(),
        emptySet<Cluster<FeatureClusterItem>>(),
      )
    )
  }

  @Test
  fun `Should force render when clusters change even if the zoom doesn't cross the threshold`() {
    featureClusterRenderer.setZoom(10f)
    featureClusterRenderer.setZoom(11f)

    val oldClusters = emptySet<Cluster<FeatureClusterItem>>()
    val newClusters =
      setOf<Cluster<FeatureClusterItem>>(
        createMockCluster(listOf(LOCATION_OF_INTEREST_CLUSTER_ITEM))
      )

    val result = featureClusterRenderer.invoke<Boolean>("shouldRender", oldClusters, newClusters)

    assertTrue(result)
  }

  private fun createMockCluster(items: List<FeatureClusterItem>): Cluster<FeatureClusterItem> {
    val cluster = mock<Cluster<FeatureClusterItem>>()
    whenever(cluster.items).thenReturn(items)
    whenever(cluster.size).thenReturn(items.size)
    whenever(cluster.position).thenReturn(LatLng(0.0, 0.0))
    return cluster
  }

  // Helper to invoke any protected method on FeatureClusterRenderer
  @Suppress("UNCHECKED_CAST")
  private fun <T> FeatureClusterRenderer.invoke(methodName: String, vararg args: Any?): T {
    val method =
      this::class.java.declaredMethods.firstOrNull { it.name == methodName }
        ?: throw IllegalArgumentException("Method $methodName not found")
    method.isAccessible = true
    return method.invoke(this, *args) as T
  }
}
