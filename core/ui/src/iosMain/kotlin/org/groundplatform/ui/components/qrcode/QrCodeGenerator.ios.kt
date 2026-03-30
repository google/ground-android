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
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package org.groundplatform.ui.components.qrcode

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreImage.CIContext
import platform.CoreImage.CIFilter
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.CoreImage.filterWithName
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue

private const val FILTER_NAME = "CIQRCodeGenerator"
private const val INPUT_MESSAGE_KEY = "inputMessage"
private const val INPUT_CORRECTION_LEVEL_KEY = "inputCorrectionLevel"

private val ciContext: CIContext = CIContext.contextWithOptions(null)

actual fun generateQrBitmap(content: String, useHighEcc: Boolean): ImageBitmap {
  val filter = CIFilter.filterWithName(FILTER_NAME) ?: error("$FILTER_NAME filter not available")
  filter.setValue(content.encodeToNSData(), forKey = INPUT_MESSAGE_KEY)
  filter.setValue(if (useHighEcc) "H" else "L", forKey = INPUT_CORRECTION_LEVEL_KEY)
  val ciImage = filter.outputImage ?: error("$FILTER_NAME produced no output")
  return ciImage.toComposeImageBitmap()
}

/**
 * Renders this [CIImage] into a Compose [ImageBitmap] by drawing the CGImage into a raw RGBA pixel
 * buffer and wrapping it as a Skia raster image.
 */
private fun CIImage.toComposeImageBitmap(): ImageBitmap {
  val cgImage =
    ciContext.createCGImage(this, fromRect = extent)
      ?: error("Failed to create CGImage from CIImage")

  val width = CGImageGetWidth(cgImage).toInt()
  val height = CGImageGetHeight(cgImage).toInt()
  val bytesPerRow = width * 4
  val data = ByteArray(bytesPerRow * height)

  data.usePinned { pinned ->
    CGBitmapContextCreate(
      data = pinned.addressOf(0),
      width = width.toULong(),
      height = height.toULong(),
      bitsPerComponent = 8u,
      bytesPerRow = bytesPerRow.toULong(),
      space = CGColorSpaceCreateDeviceRGB(),
      bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big
    )?.apply {
      CGContextDrawImage(
        this,
        CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
        cgImage
      )
    } ?: error("Failed to create bitmap context")
  }

  val imageInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
  return Image.makeRaster(imageInfo, data, bytesPerRow).toComposeImageBitmap()
}

private fun String.encodeToNSData(): NSData =
  NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)
    ?: error("Failed to encode string as UTF-8 NSData")
