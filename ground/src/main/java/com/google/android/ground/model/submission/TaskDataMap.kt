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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import java8.util.Optional

/**
 * An immutable map of task ids to related user taskDatas.
 *
 * @property taskDatas A map from task id to taskData. This map is mutable and therefore should
 * never be exposed outside this class.
 */
data class TaskDataMap
constructor(private val taskDatas: Map<String, TaskData?> = ImmutableMap.of()) {

  /**
   * Returns the user taskData for the given task id, or empty if the user did not specify a
   * taskData.
   */
  fun getResponse(taskId: String): Optional<TaskData> = Optional.ofNullable(taskDatas[taskId])

  /** Returns an Iterable over the task ids in this map. */
  fun taskIds(): Iterable<String> = taskDatas.keys

  /** Adds, replaces, and/or removes taskDatas based on the provided list of deltas. */
  fun copyWithDeltas(taskDataDeltas: ImmutableList<TaskDataDelta>): TaskDataMap {
    val newResponses = taskDatas.toMutableMap()
    taskDataDeltas.forEach {
      if (it.newTaskData.isPresent) {
        newResponses[it.taskId] = it.newTaskData.get()
      } else {
        newResponses.remove(it.taskId)
      }
    }

    return TaskDataMap(newResponses)
  }
}
