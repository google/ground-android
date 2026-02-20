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
package org.groundplatform.android.ui.datacollection.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Dimension.Pixels
import coil.size.Scale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.groundplatform.android.BaseHiltTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
@Config(qualifiers = "w480dp-h1080dp-mdpi")
class UriImageTest : BaseHiltTest() {

  private lateinit var context: Context
  private val capturedRequests = mutableListOf<ImageRequest>()

  @Before
  override fun setUp() {
    super.setUp()

    context = ApplicationProvider.getApplicationContext()
    capturedRequests.clear()

    setupImageLoader()
  }

  @After
  fun tearDown() {
    // Reset ImageLoader to avoid leaking state between tests
    Coil.setImageLoader(ImageLoader(context))
  }

  @Test
  fun uriImage_nullUri_doesNotLoadImage() = runWithTestDispatcher {
    composeTestRule.setContent { UriImage(uri = null) }

    assertThat(capturedRequests).isEmpty()
  }

  @Test
  fun uriImage_loadsImageWithCorrectSpecs() = runWithTestDispatcher {
    composeTestRule.setContent {
      UriImage(uri = URI, modifier = Modifier.requiredSize(100.dp, 100.dp))
    }

    composeTestRule.waitForIdle()

    verifyImageDimensions(100, 100)
  }

  @Test
  fun uriImage_largeBoundedSize_isCoercedToMaxImageSize() = runWithTestDispatcher {
    composeTestRule.setContent {
      UriImage(uri = URI, modifier = Modifier.requiredSize(3000.dp, 3000.dp))
    }
    composeTestRule.waitForIdle()

    verifyImageDimensions(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
  }

  @Test
  fun uriImage_unboundedSize_usesHalvedDisplayMetrics() = runWithTestDispatcher {
    composeTestRule.setContent {
      Layout(content = { UriImage(uri = URI) }) { measurables, _ ->
        val constraints = Constraints()
        val placeable = measurables[0].measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
      }
    }
    composeTestRule.waitForIdle()

    verifyImageDimensions(240, 540)
  }

  private fun setupImageLoader() {
    val imageLoader =
      ImageLoader.Builder(context)
        .components {
          add { chain ->
            capturedRequests.add(chain.request)
            SuccessResult(
              drawable =
                BitmapDrawable(
                  context.resources,
                  Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                ),
              request = chain.request,
              dataSource = DataSource.MEMORY_CACHE,
            )
          }
        }
        .build()

    Coil.setImageLoader(imageLoader)
  }

  private suspend fun verifyImageDimensions(width: Int, height: Int) {
    assertThat(capturedRequests).hasSize(1)

    val request = capturedRequests.first()
    assertThat(request.data).isEqualTo(URI)
    assertThat(request.scale).isEqualTo(Scale.FIT)
    assertThat(request.sizeResolver).isNotNull()

    // Check that transitionFactory is set (implies crossfade was called)
    assertThat(request.transitionFactory).isNotNull()

    val size = request.sizeResolver.size()
    assertThat((size.width as Pixels).px).isEqualTo(width)
    assertThat((size.height as Pixels).px).isEqualTo(height)
  }

  companion object {
    private val URI = Uri.parse("content://test/image.jpg")
  }
}
