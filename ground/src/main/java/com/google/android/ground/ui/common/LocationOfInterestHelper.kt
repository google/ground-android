/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.ui.common

import android.content.res.Resources
import com.google.android.ground.R
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.User
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import java8.util.Optional
import javax.inject.Inject

/** Common logic for formatting attributes of [LocationOfInterest] for display to the user. */
class LocationOfInterestHelper @Inject internal constructor(private val resources: Resources) {
  fun getCreatedBy(locationOfInterest: Optional<LocationOfInterest>): String =
    getUserName(locationOfInterest).map { resources.getString(R.string.added_by, it) }.orElse("")

  // TODO(#793): Allow user-defined LOI names for other LOI types.
  fun getLabel(locationOfInterest: Optional<LocationOfInterest>): String =
    locationOfInterest
      .map { loi ->
        val caption = loi.caption?.trim { it <= ' ' } ?: ""
        caption.ifEmpty { getLocationOfInterestType(loi) }
      }
      .orElse("")

  private fun getLocationOfInterestType(locationOfInterest: LocationOfInterest): String =
    when (locationOfInterest.geometry) {
      is Polygon -> "Polygon"
      is Point -> "Point"
      is LineString -> "LineString"
      is LinearRing -> "LinearRing"
      is MultiPolygon -> "MultiPolygon"
    }

  fun getSubtitle(locationOfInterest: Optional<LocationOfInterest>): String =
    locationOfInterest
      .map { resources.getString(R.string.layer_label_format, it.job.name) }
      .orElse("")

  private fun getUserName(locationOfInterest: Optional<LocationOfInterest>): Optional<String> =
    locationOfInterest.map(LocationOfInterest::created).map(AuditInfo::user).map(User::displayName)
}
