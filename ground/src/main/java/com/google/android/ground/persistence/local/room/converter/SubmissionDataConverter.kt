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
import com.google.android.ground.model.submission.SubmissionData
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import kotlinx.collections.immutable.toPersistentMap
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/** Converts between [SubmissionData] and JSON strings used to represent them in the local db. */
object SubmissionDataConverter {

  @JvmStatic
  fun toString(responseDeltas: SubmissionData): String =
    JSONObject()
      .apply {
        for (taskId in responseDeltas.taskIds()) {
          try {
            put(taskId, ValueJsonConverter.toJsonObject(responseDeltas.getValue(taskId)))
          } catch (e: JSONException) {
            Timber.e(e, "Error building JSON")
          }
        }
      }
      .toString()

  @JvmStatic
  fun fromString(job: Job, jsonString: String?): SubmissionData {
    if (jsonString == null) {
      return SubmissionData()
    }
    val map = mutableMapOf<String, TaskData>()
    try {
      val jsonObject = JSONObject(jsonString)
      val keys = jsonObject.keys()
      while (keys.hasNext()) {
        try {
          val taskId = keys.next()
          val task = job.getTask(taskId)
          ValueJsonConverter.toResponse(task, jsonObject[taskId])?.let { map[taskId] = it }
        } catch (e: LocalDataConsistencyException) {
          Timber.d("Bad submission data in local db: ${e.message}")
        } catch (e: Job.TaskNotFoundException) {
          Timber.d(e, "Ignoring data for unknown task")
        }
      }
    } catch (e: JSONException) {
      Timber.e(e, "Error parsing JSON string")
    }
    return SubmissionData(map.toPersistentMap())
  }
}
