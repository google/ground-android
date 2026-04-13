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
package org.groundplatform.ui.components.qrcode

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.groundplatform.ui.theme.sizes

@VisibleForTesting const val TEST_TAG_GROUND_QR_CODE = "TEST_TAG_GROUND_QR_CODE"

/**
 * Maximum content size (in UTF-8 bytes) for which a center logo is displayed.
 *
 * Adding a logo in the center of a QR code covers part of the data pattern, so the QR must rely on
 * error correction to remain scannable. A limit of 1,000 bytes is used as a conservative threshold
 * to ensure the QR code remains reliably scannable even with a logo applied.
 */
private const val MAX_QR_BYTES_WITH_LOGO = 1000

/**
 * The relative size of the center logo as a fraction of the QR code's total rendered size.
 *
 * Set to 15% to ensure the logo is clearly visible while remaining within the recovery capacity of
 * high error correction (ECC level H), which can tolerate approximately 30% data loss.
 */
private const val LOGO_SIZE_FRACTION = 0.15f

/**
 * Displays a QR code generated from the given [content] string.
 *
 * The composable is intentionally generic, it accepts any string payload, making it reusable for
 * GeoJSON, URLs, or any other data that fits within QR code capacity limits.
 */
@Composable
fun GroundQrCode(
  modifier: Modifier = Modifier,
  title: String,
  content: String,
  contentDescription: String,
  centerLogoPainter: Painter?,
  footer: String,
) {
  val contentBytes = remember(content) { content.encodeToByteArray().size }
  val showLogo = centerLogoPainter != null && contentBytes <= MAX_QR_BYTES_WITH_LOGO

  val qrBitmap by
    produceState<ImageBitmap?>(initialValue = null, key1 = content, key2 = showLogo) {
      value = withContext(Dispatchers.Default) { generateQrBitmap(content, showLogo) }
    }

  Column(
    modifier = modifier.fillMaxWidth().testTag(TEST_TAG_GROUND_QR_CODE),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Box(modifier = modifier.sizeIn(minWidth = 142.dp, minHeight = 142.dp)) {
      qrBitmap?.let {
        Image(
          modifier = Modifier.align(Alignment.Center),
          bitmap = it,
          contentDescription = contentDescription,
          contentScale = ContentScale.Fit,
        )
        if (showLogo) {
          Image(
            painter = centerLogoPainter,
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).fillMaxSize(LOGO_SIZE_FRACTION),
          )
        }
      }
        ?: run {
          CircularProgressIndicator(
            modifier =
              Modifier.size(MaterialTheme.sizes.progressIndicatorSize).align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = MaterialTheme.sizes.progressIndicatorStrokeWidth,
          )
        }
    }
    Text(
      text = footer,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Normal,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Preview
@Composable
private fun GroundQrCodePreview() {
  MaterialTheme {
    Surface {
      GroundQrCode(
        modifier = Modifier.padding(16.dp).size(400.dp),
        title = "Test QR code",
        content = "https://www.google.com",
        contentDescription = "Google",
        footer = "Scan this QR",
        centerLogoPainter = null,
      )
    }
  }
}
