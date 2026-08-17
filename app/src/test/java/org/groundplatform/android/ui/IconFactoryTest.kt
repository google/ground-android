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
package org.groundplatform.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class IconFactoryTest : BaseHiltTest() {
  @Inject @ApplicationContext lateinit var context: Context

  @Inject lateinit var iconFactory: IconFactory
  private val testMarker by lazy { getDrawable(context, R.drawable.ic_marker_outline) }
  private val unscaledWidth by lazy { testMarker!!.intrinsicWidth }
  private val unscaledHeight by lazy { testMarker!!.intrinsicHeight }

  private val bitmapDescriptorFactory: MockedStatic<BitmapDescriptorFactory> =
    mockStatic(BitmapDescriptorFactory::class.java).apply {
      `when`<BitmapDescriptor> { BitmapDescriptorFactory.fromBitmap(any()) }
        .thenAnswer { mock<BitmapDescriptor>() }
    }

  @After
  fun closeStaticMock() {
    bitmapDescriptorFactory.close()
  }

  @Test
  fun `getMarkerBitmap() stretches marker`() {
    val bitmap = iconFactory.getMarkerBitmap(Color.BLUE, 2.0f)

    assertBitmapScale(bitmap, 2.0f)
  }

  @Test
  fun `getMarkerBitmap() shrinks marker`() {
    val bitmap = iconFactory.getMarkerBitmap(Color.BLUE, 0.5f)

    assertBitmapScale(bitmap, 0.5f)
  }

  @Test
  fun `getMarkerIcon returns the same instance for the same color and scale`() {
    val first = iconFactory.getMarkerIcon(Color.BLUE, 2.0f)

    assertThat(iconFactory.getMarkerIcon(Color.BLUE, 2.0f)).isSameInstanceAs(first)
  }

  @Test
  fun `getMarkerIcon builds a distinct icon per color and per scale`() {
    val blue = iconFactory.getMarkerIcon(Color.BLUE, 2.0f)
    val red = iconFactory.getMarkerIcon(Color.RED, 2.0f)
    val blueSelected = iconFactory.getMarkerIcon(Color.BLUE, 3.0f)

    assertThat(red).isNotSameInstanceAs(blue)
    assertThat(blueSelected).isNotSameInstanceAs(blue)
  }

  @Test
  fun `getClusterIcon returns the same instance for the same label`() {
    iconFactory.setClusterIconCacheSize(2)
    val first = iconFactory.getClusterIcon("3/10")

    assertThat(iconFactory.getClusterIcon("4/10")).isNotSameInstanceAs(first)
    assertThat(iconFactory.getClusterIcon("3/10")).isSameInstanceAs(first)
  }

  @Test
  fun `getClusterIcon evicts the least recently used label once the cache is full`() {
    iconFactory.setClusterIconCacheSize(1)
    val first = iconFactory.getClusterIcon("3/10")
    iconFactory.getClusterIcon("4/10")

    assertThat(iconFactory.getClusterIcon("3/10")).isNotSameInstanceAs(first)
  }

  private fun assertBitmapScale(bitmap: Bitmap, scale: Float) {
    val expectedWidth = (unscaledWidth * scale).toInt()
    val expectedHeight = (unscaledHeight * scale).toInt()
    assertThat(bitmap.width to bitmap.height).isEqualTo(expectedWidth to expectedHeight)
  }
}
