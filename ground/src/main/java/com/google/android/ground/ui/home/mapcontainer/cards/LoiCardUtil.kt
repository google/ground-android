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

/** Helper class for creating user-visible text. */
object LoiCardUtil {

  fun getDisplayLoiName(context: Context, loi: LocationOfInterest): String {
    val caption = loi.caption
    val customId = loi.customId
    val geometry = loi.geometry
    return if (caption.isNotNullOrEmpty() && customId.isNotNullOrEmpty()) {
      "$caption ($customId)"
    } else if (caption.isNotNullOrEmpty()) {
      "$caption"
    } else if (customId.isNotNullOrEmpty()) {
      "${geometry.toType(context)} ($customId)"
    } else {
      geometry.toDefaultName(context)
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
