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
 * An immutable map of task ids to related user taskDatas.
 *
 * @property taskDatas A map from task id to taskData. This map is mutable and therefore should
 * never be exposed outside this class.
 */
data class TaskDataMap(private val taskDatas: Map<String, TaskData?> = mapOf()) {

  /**
   * Returns the user taskData for the given task id, or empty if the user did not specify a
   * taskData.
   */
  fun getResponse(taskId: String): TaskData? = taskDatas[taskId]

  /** Returns an Iterable over the task ids in this map. */
  fun taskIds(): Iterable<String> = taskDatas.keys

  /** Adds, replaces, and/or removes taskDatas based on the provided list of deltas. */
  fun copyWithDeltas(taskDataDeltas: List<TaskDataDelta>): TaskDataMap {
    val newResponses = taskDatas.toMutableMap()
    taskDataDeltas.forEach {
      if (it.newTaskData.isNotNullOrEmpty()) {
        newResponses[it.taskId] = it.newTaskData
      } else {
        newResponses.remove(it.taskId)
      }
    }

    return TaskDataMap(newResponses)
  }
}
