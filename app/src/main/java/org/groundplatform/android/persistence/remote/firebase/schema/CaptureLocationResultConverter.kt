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
package org.groundplatform.android.persistence.remote.firebase.schema

import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.persistence.local.room.converter.GeometryWrapperTypeConverter
import org.json.JSONObject

/** Converts between [CaptureLocationTaskData] and its equivalent [JSONObject] representation. */
object CaptureLocationResultConverter {
  private const val ACCURACY_KEY = "accuracy"
  private const val ALTITUDE_KEY = "altitude"
  private const val GEOMETRY_KEY = "geometry"

  fun CaptureLocationTaskData.toJSONObject(): JSONObject =
    JSONObject().apply {
      put(ACCURACY_KEY, accuracy)
      put(ALTITUDE_KEY, altitude)
      put(GEOMETRY_KEY, GeometryWrapperTypeConverter.toString(location))
    }

  fun JSONObject.toCaptureLocationTaskData(): CaptureLocationTaskData {
    val accuracy = getDouble(ACCURACY_KEY)
    val altitude = getDouble(ALTITUDE_KEY)
    val geometry = GeometryWrapperTypeConverter.fromString(getString(GEOMETRY_KEY))?.getGeometry()
    return CaptureLocationTaskData(geometry as Point, accuracy = accuracy, altitude = altitude)
  }
}
