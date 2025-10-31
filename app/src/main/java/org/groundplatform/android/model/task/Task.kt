/*
 * Copyright 2018 Google LLC
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
 * Describes a user-defined task.
 *
 * This contains the task definition only. Data collected for tasks in job are stored in
 * [org.groundplatform.android.model.submission.Submission].
 */
data class Task
@JvmOverloads
constructor(
  val id: String,
  /** Returns the sequential index of the task, used by UIs to sort prompts and results. */
  val index: Int,
  val type: Type,
  val label: String,
  val isRequired: Boolean,
  val multipleChoice: MultipleChoice? = null,
  val isAddLoiTask: Boolean = false,
  val condition: Condition? = null,
  /**
   * Whether the user can pan/zoom the map to place a point anywhere (true), or can only capture
   * their GPS location (false). Only applies to DROP_PIN tasks.
   */
  val allowMovingPoint: Boolean = true,
  /**
   * Whether the user can manually override the location lock by panning the map (true), or if the
   * location lock cannot be disengaged (false). Only applies to DRAW_AREA tasks. When true, the
   * location lock starts enabled but can be turned off by panning. When false, the location lock is
   * always on and map pan/zoom is disabled.
   */
  val allowManualOverride: Boolean = true,
) {

  // TODO: Define these in data layer!
  // Issue URL: https://github.com/google/ground-android/issues/2910
  // Task type names as they appear in the local db, but in uppercase. DO NOT RENAME!
  enum class Type {
    UNKNOWN,
    TEXT,
    MULTIPLE_CHOICE,
    PHOTO,
    NUMBER,
    DATE,
    TIME,
    DROP_PIN,
    DRAW_AREA,
    CAPTURE_LOCATION,
    INSTRUCTIONS,
  }
}
