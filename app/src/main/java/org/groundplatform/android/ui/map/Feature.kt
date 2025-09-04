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
package org.groundplatform.android.ui.map

import androidx.annotation.ColorInt
import org.groundplatform.android.model.geometry.Geometry

/** Represents an individual feature on a map with a given [Geometry] and [Tag]. */
data class Feature(
  val tag: Tag,
  val geometry: Geometry,
  val style: Style,
  val clusterable: Boolean,
  val selected: Boolean = false,
  /** An arbitrary slot for boolean flag. The interpretation of this field is type-dependent. */
  val flag: Boolean = false,
  val tooltipText: String? = null,
) {
  constructor(
    id: String,
    type: Type,
    geometry: Geometry,
    flag: Boolean = false,
    style: Style,
    clusterable: Boolean,
    selected: Boolean = false,
    tooltipText: String? = null,
  ) : this(Tag(id, type), geometry, style, clusterable, selected, flag, tooltipText)

  /** Tag used to uniquely identifier a feature on the map. */
  data class Tag(
    /** A unique identifier for the model object that this feature represents. */
    val id: String,
    /** Indicates how to interpret this feature as a model object. */
    val type: Type,
  )

  data class Style(@ColorInt val color: Int, val vertexStyle: VertexStyle? = VertexStyle.NONE)

  /**
   * Indicates the type of a geometric model object. Used to interpret other objects (e.g. map
   * features) back into model objects.
   */
  enum class Type {
    UNKNOWN,
    LOCATION_OF_INTEREST,
    USER_POINT,
    USER_POLYGON,
  }

  /**
   * Returns an instance [Feature] with all fields unchanged except the [selected] state, which is
   * updated to the specified value. If the [selected] state is different, a copy is returned with
   * the new value. If it's the same, the current instance is returned unchanged.
   */
  fun withSelected(newSelected: Boolean) =
    if (selected == newSelected) this else copy(selected = newSelected)

  enum class VertexStyle {
    NONE,
    CIRCLE,
  }
}
