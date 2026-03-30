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

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreImage.CIContext
import platform.CoreImage.CIFilter
import platform.CoreImage.createCGImage
import platform.CoreImage.filterWithName
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun generateQrBitmap(content: String, useHighEcc: Boolean): ImageBitmap {
  val filter =
    CIFilter.filterWithName("CIQRCodeGenerator") ?: error("CIQRCodeGenerator filter not available")
  val data: NSData = content.encodeToNSData()
  filter.setValue(data, forKey = "inputMessage")
  filter.setValue(if (useHighEcc) "H" else "L", forKey = "inputCorrectionLevel")

  val ciImage = filter.outputImage ?: error("CIQRCodeGenerator produced no output")

  val scaleX = ciImage.extent.useContents { QR_SIZE_PX.toDouble() / size.width }
  val scaleY = ciImage.extent.useContents { QR_SIZE_PX.toDouble() / size.height }
  val scaledImage = ciImage.imageByApplyingTransform(CGAffineTransformMakeScale(scaleX, scaleY))

  val context = CIContext.contextWithOptions(null)
  val cgImage =
    context.createCGImage(scaledImage, fromRect = scaledImage.extent)
      ?: error("Failed to create CGImage from scaled CIImage")

  val uiImage = UIImage.imageWithCGImage(cgImage)
  val pngData = UIImagePNGRepresentation(uiImage) ?: error("Failed to encode UIImage as PNG")

  val bytes = pngData.toByteArray()
  return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
  val length = length.toInt()
  if (length == 0) return byteArrayOf()
  return ByteArray(length).also { byteArray ->
    byteArray.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, this.length) }
  }
}

@OptIn(BetaInteropApi::class)
private fun String.encodeToNSData(): NSData =
  NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)
    ?: error("Failed to encode string as UTF-8 NSData")
