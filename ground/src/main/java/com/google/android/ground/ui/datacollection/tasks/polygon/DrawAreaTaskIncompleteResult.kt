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
package com.google.android.ground.ui.datacollection.tasks.polygon

import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.submission.GeometryTaskResult

/** User-provided "ongoing" response to a "draw an area" data collection [Task]. */
data class DrawAreaTaskIncompleteResult constructor(val lineString: LineString) :
  GeometryTaskResult(lineString) {
  override fun isEmpty(): Boolean = lineString.isEmpty()
}
