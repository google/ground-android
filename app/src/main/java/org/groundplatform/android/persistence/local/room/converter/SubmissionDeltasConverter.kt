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

package org.groundplatform.android.persistence.local.room.converter

import kotlinx.collections.immutable.toPersistentList
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.persistence.local.LocalDataConsistencyException
import org.groundplatform.android.persistence.remote.DataStoreException
import org.groundplatform.android.util.Enums.toEnum
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/** Converts between [ValueDelta] and JSON strings used to represent them in the local db. */
object SubmissionDeltasConverter {

  private const val KEY_TASK_TYPE = "taskType"
  private const val KEY_NEW_VALUE = "newValue"

  @JvmStatic
  fun toString(deltas: List<ValueDelta>): String =
    JSONObject()
      .apply {
        for (delta in deltas) {
          try {
            put(
              delta.taskId,
              JSONObject()
                .put(KEY_TASK_TYPE, delta.taskType.name)
                .put(KEY_NEW_VALUE, ValueJsonConverter.toJsonObject(delta.newTaskData)),
            )
          } catch (e: JSONException) {
            Timber.e(e, "Error building JSON")
          }
        }
      }
      .toString()

  @JvmStatic
  fun fromString(job: Job, jsonString: String?): List<ValueDelta> {
    val deltas = mutableListOf<ValueDelta>()
    if (jsonString == null) {
      return deltas.toPersistentList()
    }
    try {
      val jsonObject = JSONObject(jsonString)
      val keys = jsonObject.keys()
      while (keys.hasNext()) {
        try {
          val taskId = keys.next()
          val task = job.getTask(taskId)
          val jsonDelta = jsonObject.getJSONObject(taskId)
          deltas.add(
            ValueDelta(
              taskId,
              toEnum(Task.Type::class.java, jsonDelta.getString(KEY_TASK_TYPE)),
              ValueJsonConverter.toResponse(task, jsonDelta[KEY_NEW_VALUE]),
            )
          )
        } catch (e: LocalDataConsistencyException) {
          Timber.d("Bad submission value in local db: " + e.message)
        } catch (e: DataStoreException) {
          Timber.d("Bad submission value in local db: " + e.message)
        } catch (e: Job.TaskNotFoundException) {
          Timber.d(e, "Ignoring delta for unknown task")
        }
      }
    } catch (e: JSONException) {
      Timber.e(e, "Error parsing JSON string")
    }
    return deltas.toPersistentList()
  }
}
