/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.model.task

/**
 * Suggested configuration for a task that involves drawing geometry on a map.
 *
 * @property isLocationLockRequired whether the user's location must be locked to the map viewport
 *   before they can start drawing.
 * @property minAccuracyMeters the minimum accuracy (in meters) required for the user's location to
 *   be considered valid for drawing.
 * @property allowedMethods the list of allowed methods for drawing geometry (e.g. "DROP_PIN",
 *   "DRAW_AREA").
 */
data class DrawGeometry(
  val isLocationLockRequired: Boolean,
  val minAccuracyMeters: Float,
  val allowedMethods: List<String>,
)
