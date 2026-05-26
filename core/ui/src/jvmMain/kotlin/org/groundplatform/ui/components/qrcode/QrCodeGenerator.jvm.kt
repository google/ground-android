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

actual fun generateQrBitmap(content: String, useHighEcc: Boolean): ImageBitmap =
  // The JVM target exists only to unit-test platform-independent logic, so QR generation is
  // intentionally unimplemented here.
  throw UnsupportedOperationException("QR code generation is not supported on the JVM target")
