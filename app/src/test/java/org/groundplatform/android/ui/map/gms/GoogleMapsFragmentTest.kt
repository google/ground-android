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
package org.groundplatform.android.ui.map.gms

import android.graphics.Color
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.Test
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.features.FeatureManager
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GoogleMapsFragmentTest {

  private lateinit var fragment: GoogleMapsFragment
  private lateinit var _featureManager: FeatureManager

  @Before
  fun setup() {
    _featureManager = mock()
    fragment = GoogleMapsFragment().apply { this.featureManager = _featureManager }
  }

  @Test
  fun `non-draft features go through setFeatures`() {
    val normalFeature = fakeFeature(id = "1", isDraft = false)

    fragment.setFeatures(setOf(normalFeature))

    verify(_featureManager).setFeatures(setOf(normalFeature))
    verify(_featureManager, never()).updateLineString(any(), any(), any(), any(), anyOrNull())
  }

  @Test
  fun `first draft feature goes through setFeatures`() {
    val draft = fakeFeature(id = "draft1", isDraft = true)

    fragment.setFeatures(setOf(draft))

    verify(_featureManager).setFeatures(setOf(draft))
    verify(_featureManager, never()).updateLineString(any(), any(), any(), any(), anyOrNull())
  }

  @Test
  fun `subsequent draft updates go through updateLineString`() {
    val draft1 = fakeFeature(id = "draft1", isDraft = true)
    val draft2 = fakeFeature(id = "draft2", isDraft = true, geometry = fakeLineString(2))

    // First time → add
    fragment.setFeatures(setOf(draft1))
    // Second time → update
    fragment.setFeatures(setOf(draft2))

    // First call: setFeatures
    verify(_featureManager).setFeatures(setOf(draft1))
    // Second call: updateLineString
    verify(_featureManager)
      .updateLineString(
        eq(draft1.tag), // still stable tag
        any(),
        any(),
        any(),
        anyOrNull(),
      )
  }

  @Test
  fun `draft removal resets state`() {
    val draft = fakeFeature(id = "draft1", isDraft = true)
    val normal = fakeFeature(id = "2", isDraft = false)

    // Add draft
    fragment.setFeatures(setOf(draft))
    // Remove draft, only normal remains
    fragment.setFeatures(setOf(normal))

    verify(_featureManager).setFeatures(setOf(draft))
    verify(_featureManager).setFeatures(setOf(normal))
  }

  private fun fakeFeature(
    id: String,
    isDraft: Boolean,
    geometry: LineString = fakeLineString(1),
  ): Feature =
    Feature(
      id = id,
      type = if (isDraft) Feature.Type.USER_POLYGON else Feature.Type.USER_POINT,
      geometry = geometry,
      style = Feature.Style(color = Color.RED, vertexStyle = Feature.VertexStyle.CIRCLE),
      clusterable = !isDraft,
      selected = isDraft,
      tooltipText = if (isDraft) "draft" else null,
    )

  private fun fakeLineString(n: Int): LineString {
    val coords = (0 until n).map { Coordinates(lat = it.toDouble(), lng = it.toDouble()) }
    return LineString(coords)
  }
}
