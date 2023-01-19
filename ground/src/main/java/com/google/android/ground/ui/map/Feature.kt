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

/** Represents an individual feature on a map with a given [Geometry] and [Tag]. */
data class Feature(val tag: Tag, val geometry: Geometry) {
  /**
   * Denotes the kind of entity this map feature represents and contains any additional data it
   * carries.
   */
  data class Tag(
    /** A unique identifier for the model object that this feature represents. */
    val id: String = "",
    /** A type that indicates how to interpret this feature as a model object. */
    val type: Type,
    /** An arbitrary slot for boolean flag. The interpretation of this field is type-dependent. */
    val flag: Boolean = false
  )
  /**
   * Indicates what kind of object a given [Feature] represents. Used to determine how to interpret
   * features as model objects.
   */
  enum class Type {
    /** This feature represents an unknown kind of object. */
    UNKNOWN,
    /** This feature represents a single, discreet location on the map. */
    LOCATION_FEATURE,
  }
}
