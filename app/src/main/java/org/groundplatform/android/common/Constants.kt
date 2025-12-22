/*
 * Copyright 2019 Google LLC
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
package org.groundplatform.android.common

import android.content.Context
import org.groundplatform.android.BuildConfig
import org.groundplatform.android.model.map.MapType

/** Application constants. */
object Constants {
  // Shared preferences.
  const val SHARED_PREFS_NAME = "shared_prefs"
  const val SHARED_PREFS_MODE = Context.MODE_PRIVATE

  // Local db settings.
  const val DB_VERSION = 125
  const val DB_NAME = "ground.db"

  // Firebase Cloud Firestore settings.
  const val FIRESTORE_LOGGING_ENABLED = true

  // Photos
  const val PHOTO_EXT = ".jpg"

  // Map Settings
  /** Default zoom level used when panning and zooming the map to a specific position. */
  const val DEFAULT_LOI_ZOOM_LEVEL = 18.0f

  /**
   * Map zoom level threshold for cluster rendering. When the user is zoomed out at this level or
   * lower, renders markers as clusters, otherwise, we render them as individual markers.
   */
  const val CLUSTERING_ZOOM_THRESHOLD = 14f

  /**
   * The path segment used in deep‑link URIs to identify the survey screen.
   *
   * When handling incoming deep links, compare the first segment of the URI’s path to this constant
   * to determine whether to navigate to the survey flow.
   */
  const val SURVEY_PATH_SEGMENT = "survey"

  /** Limit on the permitted character length for free text question responses. */
  const val TEXT_DATA_CHAR_LIMIT = 255

  /** Default map type used when map is displayed. */
  val DEFAULT_MAP_TYPE = MapType.TERRAIN

  /** Accuracy threshold in meters. */
  const val ACCURACY_THRESHOLD_IN_M = 15.0f

  // TODO: Move to a util class
  fun isReleaseBuild(): Boolean = BuildConfig.BUILD_TYPE.contentEquals("release")
}
