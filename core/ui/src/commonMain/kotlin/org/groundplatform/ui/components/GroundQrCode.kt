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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import qrgenerator.qrkitpainter.PatternType
import qrgenerator.qrkitpainter.QrBallType
import qrgenerator.qrkitpainter.QrKitLogo
import qrgenerator.qrkitpainter.QrKitShapes
import qrgenerator.qrkitpainter.QrPixelType.SquarePixel
import qrgenerator.qrkitpainter.getSelectedPattern
import qrgenerator.qrkitpainter.getSelectedPixel
import qrgenerator.qrkitpainter.getSelectedQrBall
import qrgenerator.qrkitpainter.rememberQrKitPainter

/**
 * Displays a QR code generated from the given [content] string.
 *
 * The composable is intentionally generic, it accepts any string payload, making it reusable for
 * GeoJSON, URLs, or any other data that fits within QR code capacity limits.
 */
@Composable
fun GroundQrCode(
  modifier: Modifier = Modifier,
  content: String,
  contentDescription: String,
  centerLogoPainter: Painter?,
) {
  val painter =
    rememberQrKitPainter(content) {
      shapes =
        QrKitShapes(
          ballShape = getSelectedQrBall(QrBallType.SquareQrBall()),
          darkPixelShape = getSelectedPixel(SquarePixel()),
          codeShape = getSelectedPattern(PatternType.SquarePattern),
        )
      logo = QrKitLogo(centerLogoPainter)
    }
  Image(modifier = modifier, painter = painter, contentDescription = contentDescription)
}

@Preview
@Composable
private fun GroundQrCodePreview() {
  MaterialTheme {
    Surface {
      GroundQrCode(
        modifier = Modifier.padding(16.dp),
        content = "https://www.google.com",
        contentDescription = "Google",
        centerLogoPainter = null,
      )
    }
  }
}
