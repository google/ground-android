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

package com.google.android.ground.persistence.local.room.converter

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.util.Enums.toEnum
import com.google.common.collect.ImmutableList
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/** Converts between [TaskDataDelta] and JSON strings used to represent them in the local db. */
object ResponseDeltasConverter {

  private const val KEY_TASK_TYPE = "taskType"
  private const val KEY_NEW_RESPONSE = "newTaskData"

  @JvmStatic
  fun toString(taskDataDeltas: ImmutableList<TaskDataDelta>): String =
    JSONObject()
      .apply {
        for (delta in taskDataDeltas) {
          try {
            put(
              delta.taskId,
              JSONObject()
                .put(KEY_TASK_TYPE, delta.taskType.name)
                .put(
                  KEY_NEW_RESPONSE,
                  delta.newTaskData
                    .map { ResponseJsonConverter.toJsonObject(it) }
                    .orElse(JSONObject.NULL)
                )
            )
          } catch (e: JSONException) {
            Timber.e(e, "Error building JSON")
          }
        }
      }
      .toString()

  @JvmStatic
  fun fromString(job: Job, jsonString: String?): ImmutableList<TaskDataDelta> {
    val deltas = ImmutableList.builder<TaskDataDelta>()
    if (jsonString == null) {
      return deltas.build()
    }
    try {
      val jsonObject = JSONObject(jsonString)
      val keys = jsonObject.keys()
      while (keys.hasNext()) {
        try {
          val taskId = keys.next()
          val task =
            job.getTask(taskId).orElseThrow {
              LocalDataConsistencyException("Unknown task id $taskId")
            }
          val jsonDelta = jsonObject.getJSONObject(taskId)
          deltas.add(
            TaskDataDelta(
              taskId,
              toEnum(Task.Type::class.java, jsonDelta.getString(KEY_TASK_TYPE)),
              ResponseJsonConverter.toResponse(task, jsonDelta[KEY_NEW_RESPONSE])
            )
          )
        } catch (e: LocalDataConsistencyException) {
          Timber.d("Bad taskData in local db: " + e.message)
        } catch (e: DataStoreException) {
          Timber.d("Bad taskData in local db: " + e.message)
        }
      }
    } catch (e: JSONException) {
      Timber.e(e, "Error parsing JSON string")
    }
    return deltas.build()
  }
}
