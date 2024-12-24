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
package com.google.android.ground.model.submission

import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.task.Task

/** A user-provided response to a geometry-based task ("drop a pin" or "draw an area"). */
sealed class GeometryTaskData(val geometry: Geometry) : TaskData

/** User-provided response to a "drop a pin" data collection [Task]. */
data class DropPinTaskData(val location: Point) : GeometryTaskData(location) {
  override fun isEmpty(): Boolean = false
}

/** User-provided response to a "draw an area" data collection [Task]. */
data class DrawAreaTaskData(val area: Polygon) : GeometryTaskData(area) {
  override fun isEmpty(): Boolean = area.isEmpty()
}

/** User-provided "ongoing" response to a "draw an area" data collection [Task]. */
data class DrawAreaTaskIncompleteData(val lineString: LineString) : GeometryTaskData(lineString) {
  override fun isEmpty(): Boolean = lineString.isEmpty()
}
