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

package org.groundplatform.android.util.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TileImageTransformerTest {

  @Test
  fun `setTransparentIf makes matching pixels transparent`() {
    val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    bitmap.setPixel(0, 0, Color.RED)
    bitmap.setPixel(1, 0, Color.GREEN)
    bitmap.setPixel(0, 1, Color.BLUE)
    bitmap.setPixel(1, 1, Color.WHITE)

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val inputBytes = outputStream.toByteArray()

    val resultBytes =
      TileImageTransformer.setTransparentIf(inputBytes) { bmp, x, y ->
        bmp.getPixel(x, y) == Color.RED
      }

    val resultBitmap = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.size)
    assertThat(resultBitmap).isNotNull()
    assertThat(resultBitmap.width).isEqualTo(2)
    assertThat(resultBitmap.height).isEqualTo(2)

    assertThat(Color.alpha(resultBitmap.getPixel(0, 0))).isEqualTo(0)
    assertThat(Color.alpha(resultBitmap.getPixel(1, 0))).isGreaterThan(0)
  }
}
