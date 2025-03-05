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
package org.groundplatform.android.ui.common

import android.content.res.Resources
import org.groundplatform.android.R
import org.groundplatform.android.model.geometry.Geometry
import org.groundplatform.android.model.geometry.MultiPolygon
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.locationofinterest.LOI_NAME_PROPERTY
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import javax.inject.Inject

/** Helper class for creating user-visible text. */
class LocationOfInterestHelper @Inject internal constructor(private val resources: Resources) {

  fun getDisplayLoiName(loi: LocationOfInterest): String {
    val loiId = loi.customId.ifEmpty { loi.getProperty("id") }
    val loiName = loi.getProperty(LOI_NAME_PROPERTY)

    return when {
      loiName.isNotEmpty() && loiId.isNotEmpty() -> "$loiName ($loiId)"
      loiName.isNotEmpty() -> loiName
      loiId.isNotEmpty() -> "${loi.geometry.toDefaultName()} ($loiId)"
      else -> loi.geometry.toDefaultName()
    }
  }

  fun getJobName(loi: LocationOfInterest): String? = loi.job.name

  /** Returns a default user-visible name for the geometry. */
  private fun Geometry.toDefaultName(): String =
    when (this) {
      is Point -> resources.getString(R.string.unnamed_point)
      is Polygon,
      is MultiPolygon -> resources.getString(R.string.unnamed_area)
      else -> throw IllegalArgumentException("Unsupported geometry type $this")
    }
}
