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
package org.groundplatform.android.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.io.FileOutputStream
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GlideDownsampleExifTest {

  @Test
  fun loadsLargeExifRotatedImage_downsamplesAndRotates() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val file =
      createLargeJpegWithExif(context.cacheDir, 4000, 3000, ExifInterface.ORIENTATION_ROTATE_90)
    val uri = Uri.fromFile(file)

    val targetW = 1080
    val targetH = 1920

    val bmp =
      withContext(Dispatchers.IO) {
        val future =
          Glide.with(context)
            .asBitmap()
            .load(uri)
            .override(targetW, targetH)
            .downsample(DownsampleStrategy.AT_MOST)
            .submit() // or .submit(targetW, targetH)

        try {
          future.get()
        } finally {
          Glide.with(context).clear(future)
        }
      }

    assertThat(bmp.width).isAtMost(targetW)
    assertThat(bmp.height).isAtMost(targetH)
    assertThat(bmp.height).isGreaterThan(bmp.width) // 90° rotation ⇒ taller than wide
    bmp.recycle()
  }

  private fun createLargeJpegWithExif(dir: File, w: Int, h: Int, orientation: Int): File {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(bmp).drawColor(Color.MAGENTA)
    val out = File(dir, "exif_${System.currentTimeMillis()}.jpg")
    FileOutputStream(out).use { fos -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos) }
    bmp.recycle()
    ExifInterface(out.absolutePath).apply {
      setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
      saveAttributes()
    }
    return out
  }
}
