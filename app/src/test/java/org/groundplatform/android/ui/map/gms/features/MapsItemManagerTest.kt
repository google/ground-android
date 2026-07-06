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

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.Point
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

@RunWith(RobolectricTestRunner::class)
class MapsItemManagerTest {
  private val map: GoogleMap = mock()
  private val pointRenderer: PointRenderer = mock()
  private val polygonRenderer: PolygonRenderer = mock()
  private val lineStringRenderer: LineStringRenderer = mock()

  private lateinit var mapsItemManager: MapsItemManager

  @Before
  fun setUp() {
    mapsItemManager = MapsItemManager(map, pointRenderer, polygonRenderer, lineStringRenderer)
  }

  @Test
  fun `update() returns false when the feature tag has no rendered items`() {
    assertThat(mapsItemManager.update(TEST_LINE_STRING_FEATURE)).isFalse()
    verify(lineStringRenderer, never()).update(any(), any(), any(), anyOrNull())
  }

  @Test
  fun `update() updates a rendered line string and returns true`() {
    val polyline = mock<Polyline>()
    whenever(lineStringRenderer.add(any(), any(), any(), any(), any(), any(), anyOrNull()))
      .thenReturn(polyline)
    mapsItemManager.put(TEST_LINE_STRING_FEATURE, visible = true)

    val moved =
      TEST_LINE_STRING_FEATURE.copy(
        geometry = LineString(listOf(Coordinates(0.0, 0.0), Coordinates(1.0, 1.0)))
      )
    val result = mapsItemManager.update(moved)

    assertThat(result).isTrue()
    verify(lineStringRenderer)
      .update(eq(map), eq(polyline), eq(moved.geometry as LineString), anyOrNull())
  }

  @Test
  fun `update() returns false for unsupported item types`() {
    whenever(pointRenderer.add(any(), any(), any(), any(), any(), any(), anyOrNull()))
      .thenReturn(mock<Marker>())
    mapsItemManager.put(TEST_POINT_FEATURE, visible = true)

    assertThat(mapsItemManager.update(TEST_POINT_FEATURE)).isFalse()
  }

  private companion object {
    val TEST_LINE_STRING_FEATURE =
      Feature(
        tag = Feature.Tag("line", Feature.Type.USER_POLYGON),
        geometry = LineString(listOf(Coordinates(0.0, 0.0), Coordinates(1.0, 1.0))),
        style = Feature.Style(0),
        clusterable = false,
      )
    val TEST_POINT_FEATURE =
      Feature(
        tag = Feature.Tag("point", Feature.Type.USER_POINT),
        geometry = Point(Coordinates(0.0, 0.0)),
        style = Feature.Style(0),
        clusterable = false,
      )
  }
}
