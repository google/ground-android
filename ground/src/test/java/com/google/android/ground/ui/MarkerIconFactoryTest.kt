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
package com.google.android.ground.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.Config
import com.google.android.ground.R
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MarkerIconFactoryTest : BaseHiltTest() {
  @Inject @ApplicationContext lateinit var context: Context

  @Inject lateinit var markerIconFactory: IconFactory
  private var markerUnscaledWidth = 0
  private var markerUnscaledHeight = 0

  @Before
  override fun setUp() {
    super.setUp()
    val outline = AppCompatResources.getDrawable(context, R.drawable.ic_marker_outline)
    markerUnscaledWidth = outline!!.intrinsicWidth
    markerUnscaledHeight = outline.intrinsicHeight
  }

  @Test
  fun markerBitmap_zoomedOut_scaleIsSetCorrectly() {
    val bitmap = markerIconFactory.getMarkerBitmap(Color.BLUE, Config.ZOOM_LEVEL_THRESHOLD - 0.1f)

    val scale = ResourcesCompat.getFloat(context.resources, R.dimen.default_marker_scale_factor)
    verifyBitmapScale(bitmap, scale)
  }

  @Test
  fun markerBitmap_zoomedIn_scaleIsSetCorrectly() {
    val bitmap = markerIconFactory.getMarkerBitmap(Color.BLUE, Config.ZOOM_LEVEL_THRESHOLD)

    val scale = ResourcesCompat.getFloat(context.resources, R.dimen.zoomed_marker_scale_factor)
    verifyBitmapScale(bitmap, scale)
  }

  @Test
  fun markerBitmap_zoomedOut_isSelected_scaleIsSetCorrectly() {
    val bitmap =
      markerIconFactory.getMarkerBitmap(Color.BLUE, Config.ZOOM_LEVEL_THRESHOLD - 0.1f, true)

    val scale = ResourcesCompat.getFloat(context.resources, R.dimen.default_marker_scale_factor) + 1
    verifyBitmapScale(bitmap, scale)
  }

  @Test
  fun markerBitmap_zoomedIn_isSelected_scaleIsSetCorrectly() {
    val bitmap = markerIconFactory.getMarkerBitmap(Color.BLUE, Config.ZOOM_LEVEL_THRESHOLD, true)

    val scale = ResourcesCompat.getFloat(context.resources, R.dimen.zoomed_marker_scale_factor) + 1
    verifyBitmapScale(bitmap, scale)
  }

  private fun verifyBitmapScale(bitmap: Bitmap, scale: Float) {
    val expectedWidth = (markerUnscaledWidth * scale).toInt()
    val expectedHeight = (markerUnscaledHeight * scale).toInt()
    assertThat(bitmap.width).isEqualTo(expectedWidth)
    assertThat(bitmap.height).isEqualTo(expectedHeight)
  }
}
