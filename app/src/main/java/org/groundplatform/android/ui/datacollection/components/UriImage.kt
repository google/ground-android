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

import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

const val MAX_IMAGE_SIZE = 2048

@Composable
fun UriImage(uri: Uri?, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val displayMetrics = LocalResources.current.displayMetrics

  // Determine target dimensions to avoid loading very large images into memory.
  // - Prefer the view’s measured size if available, else fall back to half the screen size.
  // - Clamp to a maximum of 2048px to keep decoding efficient and prevent OOM on huge images.
  BoxWithConstraints(modifier = modifier) {
    val measureW =
      if (constraints.hasBoundedWidth) constraints.maxWidth else displayMetrics.widthPixels / 2
    val measureH =
      if (constraints.hasBoundedHeight) constraints.maxHeight else displayMetrics.heightPixels / 2

    val targetW = measureW.coerceAtMost(MAX_IMAGE_SIZE)
    val targetH = measureH.coerceAtMost(MAX_IMAGE_SIZE)

    AsyncImage(
      model =
        ImageRequest.Builder(context)
          .data(uri)
          .size(width = targetW, height = targetH)
          .scale(Scale.FIT)
          .placeholder(R.drawable.ic_photo_grey_600_24dp)
          .error(R.drawable.outline_error_outline_24)
          .crossfade(true)
          .build(),
      contentDescription = stringResource(id = R.string.photo_preview),
      contentScale = ContentScale.Fit,
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun UriImagePreview() {
  AppTheme { UriImage(uri = "content://media/external/images/media/1".toUri()) }
}
