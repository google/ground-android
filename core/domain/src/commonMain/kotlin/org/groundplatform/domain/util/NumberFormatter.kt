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
package org.groundplatform.domain.util

import kotlin.math.absoluteValue
import kotlin.math.round

fun Double.toFixedDecimals(decimals: Int): String {
  if (decimals <= 0) return round(this).toLong().toString()
  var scale = 1L
  repeat(decimals) { scale *= 10 }
  val scaled = round(this * scale).toLong()
  val sign = if (scaled < 0) "-" else ""
  val absScaled = scaled.absoluteValue
  val intPart = absScaled / scale
  val fracPart = (absScaled % scale).toString().padStart(decimals, '0')
  return "$sign$intPart.$fracPart"
}
