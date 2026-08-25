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

import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon as MapsPolygon
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm
import kotlinx.coroutines.test.TestScope
import org.groundplatform.android.ui.IconFactory
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.Polygon
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FeatureManagerTest {
  private val map: GoogleMap = mock()
  private val pointRenderer: PointRenderer = mock()
  private val polygonRenderer: PolygonRenderer = mock()
  private val lineStringRenderer: LineStringRenderer = mock()
  private val mapsPolygon: MapsPolygon = mock()

  private lateinit var featureManager: FeatureManager

  @Suppress("UNCHECKED_CAST")
  private val clusterAlgorithm
    get() =
      featureManager.clusterManager.gmsClusterManager.algorithm
        as NonHierarchicalViewBasedAlgorithm<FeatureClusterItem>

  @Before
  fun setUp() {
    whenever(map.cameraPosition).thenReturn(CameraPosition(LatLng(0.0, 0.0), 10f, 0f, 0f))
    whenever(polygonRenderer.add(any(), any(), any(), any(), any(), any(), anyOrNull()))
      .thenReturn(mapsPolygon)
    featureManager =
      FeatureManager(
        ApplicationProvider.getApplicationContext(),
        TestScope(),
        pointRenderer,
        polygonRenderer,
        lineStringRenderer,
        IconFactory(ApplicationProvider.getApplicationContext()),
      )
    featureManager.onMapReady(map)
  }

  @Test
  fun `does not render clusterable features to the map right when they are added`() {
    featureManager.setFeatures(listOf(clusterableFeature("a"), clusterableFeature("b")))

    verify(polygonRenderer, never()).add(any(), any(), any(), any(), any(), any(), anyOrNull())
  }

  @Test
  fun `renders non-clusterable features to the map as soon as they are added`() {
    featureManager.setFeatures(listOf(clusterableFeature("a").copy(clusterable = false)))

    verify(polygonRenderer).add(any(), any(), any(), any(), any(), any(), anyOrNull())
  }

  @Test
  fun `only features within the visible area reach the renderer`() {
    featureManager.setFeatures(
      listOf(
        clusterableFeature("near"),
        clusterableFeature("far").copy(geometry = FAR_FROM_THE_DEFAULT_POSITION),
      )
    )

    val reachingRenderer = clusterAlgorithm.getClusters(ZOOM).flatMap { it.items }

    assertThat(reachingRenderer.map { it.feature.tag.id }).containsExactly("near")
  }

  @Test
  fun `features reach the renderer as they come into view`() {
    featureManager.setFeatures(
      listOf(
        clusterableFeature("near"),
        clusterableFeature("far").copy(geometry = FAR_FROM_THE_DEFAULT_POSITION),
      )
    )

    clusterAlgorithm.onCameraChange(CameraPosition(LatLng(60.0, 60.0), ZOOM, 0f, 0f))
    val reachingRenderer = clusterAlgorithm.getClusters(ZOOM).flatMap { it.items }

    assertThat(reachingRenderer.map { it.feature.tag.id }).containsExactly("far")
  }

  @Test
  fun `draws a feature individually when the renderer reports it as unclustered`() {
    val feature = clusterableFeature("a")
    featureManager.setFeatures(listOf(feature))

    featureManager.clusterRenderer.onClusterItemRendered(feature.tag)

    verify(polygonRenderer)
      .add(
        map = any(),
        tag = eq(feature.tag),
        geometry = any(),
        style = any(),
        selected = any(),
        visible = any(),
        tooltipText = anyOrNull(),
      )
  }

  @Test
  fun `releases an individually drawn feature's map item when it becomes clustered`() {
    val feature = clusterableFeature("a")
    featureManager.setFeatures(listOf(feature))
    featureManager.clusterRenderer.onClusterItemRendered(feature.tag)

    featureManager.clusterRenderer.onClusterRendered(feature.tag)

    verify(mapsPolygon).remove()
  }

  @Test
  @Config(qualifiers = "w360dp-h800dp-xxhdpi")
  fun `counts the clusters that fit on a phone screen`() {
    // 360dp / 100dp = 3.6 columns, 800dp / 100dp = 8 rows, 3.6 * 8 = 28.8 cells.
    assertThat(featureManager.maxVisibleClusters(clusterAlgorithm)).isEqualTo(28)
  }

  @Test
  @Config(qualifiers = "w360dp-h800dp-mdpi")
  fun `counts the clusters that fit on screen independently of density`() {
    assertThat(featureManager.maxVisibleClusters(clusterAlgorithm)).isEqualTo(28)
  }

  @Test
  @Config(qualifiers = "w1280dp-h800dp-xhdpi")
  fun `counts the clusters that fit on a tablet screen`() {
    // 1280dp / 100dp = 12.8 columns, 800dp / 100dp = 8 rows, 12.8 * 8 = 102.4 cells.
    assertThat(featureManager.maxVisibleClusters(clusterAlgorithm)).isEqualTo(102)
  }

  private fun clusterableFeature(id: String) =
    Feature(
      tag = Feature.Tag(id, Feature.Type.LOCATION_OF_INTEREST),
      geometry =
        Polygon(
          LinearRing(
            listOf(
              Coordinates(0.0, 0.0),
              Coordinates(0.0, 1.0),
              Coordinates(1.0, 1.0),
              Coordinates(0.0, 0.0),
            )
          )
        ),
      style = Feature.Style(0),
      clusterable = true,
    )

  private companion object {
    const val ZOOM = 5f
    val FAR_FROM_THE_DEFAULT_POSITION =
      Polygon(
        LinearRing(
          listOf(
            Coordinates(60.0, 60.0),
            Coordinates(60.0, 61.0),
            Coordinates(61.0, 61.0),
            Coordinates(60.0, 60.0),
          )
        )
      )
  }
}
