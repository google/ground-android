/*
 * Copyright 2023 Google LLC
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

package org.groundplatform.android.util

import java.io.File
import kotlin.math.ceil

typealias ByteCount = Int

typealias MegabyteCount = Float

/** Return true iff the file is non-null and contains other files. */
fun File?.isEmpty() = this?.listFiles().isNullOrEmpty()

/** Deletes the file if itt is non-null and doesn't contain other files. */
fun File.deleteIfEmpty() {
  if (isEmpty()) delete()
}

/** Returns the byte count as an equivalent megabyte count. */
fun ByteCount.toMb(): MegabyteCount = this / (1024f * 1024f)

/**
 * Returns the number of megabytes a string, replacing smaller sizes with "<1" and rounding up
 * others.
 */
fun MegabyteCount.toMbString(): String = if (this < 1) "<1" else ceil(this).toInt().toString()
