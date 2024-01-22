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
package com.google.android.ground.ui.home.mapcontainer.cards

import android.content.Context
import com.google.android.ground.R
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.util.isNotNullOrEmpty
import kotlin.math.floor

/** Helper class for creating user-visible text. */
object LoiCardUtil {

  fun getDisplayLoiName(context: Context, loi: LocationOfInterest): String {
    val loiId =
      if (loi.customId.isNotNullOrEmpty()) loi.customId
      else loi.properties["id"]?.toString()?.formatAsId()
    val geometry = loi.geometry
    val name: String? = loi.properties["name"]?.toString()
    return if (name.isNotNullOrEmpty() && loiId.isNotNullOrEmpty()) {
      "$name ($loiId)"
    } else if (name.isNotNullOrEmpty()) {
      "$name"
    } else if (loiId.isNotNullOrEmpty()) {
      "${geometry.toType(context)} ($loiId)"
    } else {
      geometry.toDefaultName(context)
    }
  }

  /**
   * Converts float/double IDs into ints if the decimal portion is 0; ignores all other id types.
   * * ex. "1234.00" -> "1234",
   * * "1234.01" -> "1234.01",
   * * "abcdefg" -> "abcdefg",
   */
  private fun String.formatAsId(): String {
    return when {
      toDoubleOrNull() != null && toDouble() - floor(toDouble()) == 0.0 -> {
        String.format("%.0f", toDouble())
      }
      else -> {
        this
      }
    }
  }

  fun getJobName(loi: LocationOfInterest): String? = loi.job.name

  fun getSubmissionsText(count: Int): String =
    when (count) {
      0 -> "No submissions"
      1 -> "$count submission"
      else -> "$count submissions"
    }

  /** Returns a user-visible string representing the type of the geometry. */
  private fun Geometry.toType(context: Context): String =
    when (this) {
      is Point -> context.getString(R.string.point)
      is Polygon,
      is MultiPolygon -> context.getString(R.string.area)
      else -> throw IllegalArgumentException("Unsupported geometry type $this")
    }

  /** Returns a default user-visible name for the geometry. */
  private fun Geometry.toDefaultName(context: Context): String =
    when (this) {
      is Point -> context.getString(R.string.unnamed_point)
      is Polygon,
      is MultiPolygon -> context.getString(R.string.unnamed_area)
      else -> throw IllegalArgumentException("Unsupported geometry type $this")
    }
}
