/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.util

import org.groundplatform.domain.model.map.MapType

/** Global constants across domain models, spatial calculations, and map settings. */
object Constants {
  // Area unit conversions
  const val SQUARE_METERS_PER_ACRE = 4046.86
  const val SQUARE_METERS_PER_HECTARE = 10_000
  const val SQUARE_FEET_PER_SQUARE_METER = 10.7639

  // Map settings
  /** Default zoom level used when panning and zooming the map to a specific position. */
  const val DEFAULT_LOI_ZOOM_LEVEL = 18.0f

  /**
   * Map zoom level threshold for cluster rendering. When the user is zoomed out at this level or
   * lower, renders markers as clusters, otherwise, we render them as individual markers.
   */
  const val CLUSTERING_ZOOM_THRESHOLD = 14f

  /** Limit on the permitted character length for free text question responses. */
  const val TEXT_DATA_CHAR_LIMIT = 255

  /** Default map type used when map is displayed. */
  val DEFAULT_MAP_TYPE = MapType.TERRAIN

  /** Accuracy threshold in meters. */
  const val ACCURACY_THRESHOLD_IN_M = 15.0f
}
