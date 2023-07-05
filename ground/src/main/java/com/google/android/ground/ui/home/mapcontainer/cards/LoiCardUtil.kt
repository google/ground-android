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

import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.locationofinterest.LocationOfInterest

/** Helper class for creating user-visible text. */
object LoiCardUtil {

  fun getDisplayLoiName(loi: LocationOfInterest): String = loi.caption ?: loi.geometry.type()

  fun getJobName(loi: LocationOfInterest): String? = loi.job.name

  fun getSubmissionsText(count: Int): String =
    when (count) {
      0 -> "No submissions"
      1 -> "$count submission"
      else -> "$count submissions"
    }

  /** Returns a user-visible string representing the type of the geometry. */
  private fun Geometry.type() =
    when (this) {
      is Point -> "Point"
      is Polygon -> "Polygon"
      is LinearRing -> "LinearRing"
      is LineString -> "LineString"
      is MultiPolygon -> "MultiPolygon"
    }
}
