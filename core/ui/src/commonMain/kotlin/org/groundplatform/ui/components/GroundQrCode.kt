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
package org.groundplatform.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.groundplatform.ui.theme.AppTheme

/**
 * Displays a QR code generated from the given [qrContent] string.
 *
 * The composable is intentionally generic, it accepts any string payload, making it reusable for
 * GeoJSON, URLs, or any other data that fits within QR code capacity limits.
 */
@Composable
fun GroundQrCode(
  modifier: Modifier = Modifier,
  title: String,
  qrContent: String,
  contentDescription: String,
  centerLogoPainter: Painter?,
  footer: String,
) {
  val qrcodePainter =
    rememberQrCodePainter(data = qrContent) {
      if (centerLogoPainter != null) {
        logo { painter = centerLogoPainter }
      }
    }

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Image(
      modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).size(142.dp),
      painter = qrcodePainter,
      contentDescription = contentDescription,
    )
    Text(text = footer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
  }
}

@Preview
@Composable
private fun GroundQrCodePreview() {
  AppTheme {
    Surface {
      GroundQrCode(
        modifier = Modifier.padding(16.dp),
        title = "Test QR Code",
        qrContent = "https://www.google.com",
        contentDescription = "Google",
        footer = "Scan this QR",
        centerLogoPainter = null,
      )
    }
  }
}
