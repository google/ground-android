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
package com.google.android.ground.ui.map

import com.google.android.ground.model.geometry.Geometry

/** Represents an individual feature on a map with a given [Geometry]. */
data class Feature(val tag: Tag, val geometry: Geometry) {
  /** Denotes the kind of entity this map feature represents. */
  sealed class Tag(open val id: String)
  /** A [Feature.Tag] that indicates this map feature represents a location of interest. */
  data class LocationOfInterestTag(
    override val id: String,
    val caption: String,
    val lastModified: String
  ) : Tag(id)
  /** A [Feature.Tag] that indicates this map feature represents a polygon drawn by the user. */
  data class UserPolygonTag(override val id: String) : Tag(id)
}
