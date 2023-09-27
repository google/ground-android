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
package com.google.android.ground.model.task

/**
 * Describes a user-defined task. Does not contain actual task responses (see [TaskData] instead.
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
  val multipleChoice: MultipleChoice? = null
) {
  /**
   * Task type names as they appear in the remote db, but in uppercase. DO NOT RENAME! TODO: Define
   * these in data layer!
   */
  enum class Type {
    UNKNOWN,
    TEXT,
    MULTIPLE_CHOICE,
    PHOTO,
    NUMBER,
    DATE,
    TIME,
    DROP_A_PIN,
    DRAW_POLYGON,
    CAPTURE_LOCATION
  }
}
