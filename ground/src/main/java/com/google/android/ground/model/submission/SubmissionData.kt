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
package com.google.android.ground.model.submission

/**
 * An immutable map of task ids to submitted data values.
 *
 * @property data A map from task id to values. This map is mutable and therefore should never be
 *   exposed outside this class.
 */
data class SubmissionData(private val data: Map<String, TaskData?> = mapOf()) {

  /**
   * Returns the submitted value for the task with the given id, or empty if the user did not
   * specify a value.
   */
  fun getValue(taskId: String): TaskData? = data[taskId]

  /** Returns an Iterable over the task ids in this map. */
  fun taskIds(): Iterable<String> = data.keys

  /** Adds, replaces, and/or removes values based on the provided list of deltas. */
  fun copyWithDeltas(deltas: List<ValueDelta>): SubmissionData {
    val newData = data.toMutableMap()
    deltas.forEach {
      if (it.newTaskData.isNotNullOrEmpty()) {
        newData[it.taskId] = it.newTaskData
      } else {
        newData.remove(it.taskId)
      }
    }

    return SubmissionData(newData)
  }
}
