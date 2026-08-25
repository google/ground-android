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
package org.groundplatform.feature.pdf

import android.graphics.Bitmap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.feature.pdf.render.image.PdfImageSet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidPdfImageProviderTest {

  @Test
  fun `calculateInSampleSize does not subsample when image already fits within 2x`() {
    assertEquals(1, calculateInSampleSize(width = 100, height = 100, maxWidth = 60, maxHeight = 60))
  }

  @Test
  fun `calculateInSampleSize halves a square image down towards the box`() {
    assertEquals(2, calculateInSampleSize(width = 100, height = 100, maxWidth = 50, maxHeight = 50))
  }

  @Test
  fun `calculateInSampleSize subsamples a typical landscape photo`() {
    assertEquals(
      2,
      calculateInSampleSize(width = 4000, height = 3000, maxWidth = 1346, maxHeight = 1108),
    )
  }

  @Test
  fun `calculateInSampleSize subsamples a tall image on its binding axis`() {
    assertEquals(
      4,
      calculateInSampleSize(width = 1000, height = 5000, maxWidth = 1346, maxHeight = 1108),
    )
  }

  @Test
  fun `calculateInSampleSize never upsamples a tiny image`() {
    assertEquals(1, calculateInSampleSize(width = 10, height = 10, maxWidth = 50, maxHeight = 50))
  }

  @Test
  fun `calculateInSampleSize leaves less than a 2x downscale for any input`() {
    val maxWidth = 1346
    val maxHeight = 1108
    val dimensions = listOf(5000 to 1000, 1000 to 5000, 4000 to 3000, 3000 to 4000, 8000 to 8000)
    for ((width, height) in dimensions) {
      val sampleSize = calculateInSampleSize(width, height, maxWidth, maxHeight)
      val decodedWidth = width / sampleSize
      val decodedHeight = height / sampleSize
      val fitScale = minOf(maxWidth.toFloat() / decodedWidth, maxHeight.toFloat() / decodedHeight)
      assertTrue(fitScale > 0.5f)
    }
  }

  @Test
  fun `scaledToFit returns the same bitmap when it already fits`() {
    val bitmap = bitmap(10, 10)
    assertSame(bitmap, bitmap.scaledToFit(maxWidth = 50, maxHeight = 50))
  }

  @Test
  fun `scaledToFit returns the same bitmap when it exactly matches the box`() {
    val bitmap = bitmap(50, 50)
    assertSame(bitmap, bitmap.scaledToFit(maxWidth = 50, maxHeight = 50))
  }

  @Test
  fun `scaledToFit downscales preserving aspect ratio`() {
    val result = bitmap(100, 50).scaledToFit(maxWidth = 50, maxHeight = 50)
    assertEquals(50, result.width)
    assertEquals(25, result.height)
  }

  @Test
  fun `scaledToFit fits to the binding height when the box is wide`() {
    val result = bitmap(50, 100).scaledToFit(maxWidth = 50, maxHeight = 50)
    assertEquals(25, result.width)
    assertEquals(50, result.height)
  }

  @Test
  fun `load generates a qr image when content is provided`() = runTest {
    val images = newProvider().load(qrContent = "https://example.org", photoFilenames = emptySet())

    assertNotNull(images[PdfImageSet.ImageRef.Qr])
  }

  @Test
  fun `load returns no qr image when content is null`() = runTest {
    val images = newProvider().load(qrContent = null, photoFilenames = emptySet())

    assertNull(images[PdfImageSet.ImageRef.Qr])
  }

  @Test
  fun `load skips empty photo filenames`() = runTest {
    val images = newProvider().load(qrContent = null, photoFilenames = setOf(""))

    assertNull(images[PdfImageSet.ImageRef.Photo("")])
  }

  @Test
  fun `load skips photos whose file does not exist`() = runTest {
    val images = newProvider().load(qrContent = null, photoFilenames = setOf("missing.jpg"))

    assertNull(images[PdfImageSet.ImageRef.Photo("missing.jpg")])
  }

  @Test
  fun `release recycles the bitmaps it loaded`() = runTest {
    val images = newProvider().load(qrContent = "https://example.org", photoFilenames = emptySet())
    val qrBitmap = images[PdfImageSet.ImageRef.Qr]!!.bitmap
    assertFalse(qrBitmap.isRecycled)

    images.release()

    assertTrue(qrBitmap.isRecycled)
  }

  private fun newProvider(): AndroidPdfImageProvider =
    AndroidPdfImageProvider(RuntimeEnvironment.getApplication(), logoDrawableRes = 0)

  private fun bitmap(width: Int, height: Int): Bitmap =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
}
