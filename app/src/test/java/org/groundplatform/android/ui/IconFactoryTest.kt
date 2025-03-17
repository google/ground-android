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
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class IconFactoryTest : BaseHiltTest() {
  @Inject @ApplicationContext lateinit var context: Context

  @Inject lateinit var iconFactory: IconFactory
  private val testMarker by lazy { getDrawable(context, R.drawable.ic_marker_outline) }
  private val unscaledWidth by lazy { testMarker!!.intrinsicWidth }
  private val unscaledHeight by lazy { testMarker!!.intrinsicHeight }

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

  private fun assertBitmapScale(bitmap: Bitmap, scale: Float) {
    val expectedWidth = (unscaledWidth * scale).toInt()
    val expectedHeight = (unscaledHeight * scale).toInt()
    assertThat(bitmap.width to bitmap.height).isEqualTo(expectedWidth to expectedHeight)
  }
}
