/*
 * Copyright 2024 Google LLC
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

import android.location.Location
import kotlin.math.abs
import org.groundplatform.android.model.geometry.Coordinates

/** Converts the given coordinates in decimal format to D°M′S″ format. */
fun Coordinates.toDmsFormat(): String = "${convertLatToDms(lat)} ${convertLongToDms(lng)}"

private fun convertLatToDms(lat: Double): String {
  val orientation = if (lat > 0) "N" else "S"
  return "${decimalToDms(lat)} $orientation"
}

private fun convertLongToDms(long: Double): String {
  val orientation = if (long > 0) "E" else "W"
  return "${decimalToDms(long)} $orientation"
}

private fun decimalToDms(latOrLong: Double): String {
  val dmsFormat = Location.convert(abs(latOrLong), Location.FORMAT_SECONDS)
  val (degrees, minutes, seconds) = dmsFormat.split(":".toRegex())
  return "$degrees°$minutes'$seconds\""
}
