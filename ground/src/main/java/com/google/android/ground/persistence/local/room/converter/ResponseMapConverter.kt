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
import com.google.android.ground.model.submission.Response
import com.google.android.ground.model.submission.ResponseMap
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.common.collect.ImmutableMap
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/** Converts between [ResponseMap] and JSON strings used to represent them in the local db.  */
object ResponseMapConverter {

    @JvmStatic
    fun toString(responseDeltas: ResponseMap): String = JSONObject().apply {
        for (taskId in responseDeltas.taskIds()) {
            try {
                put(taskId,
                    responseDeltas.getResponse(taskId)
                        .map { ResponseJsonConverter.toJsonObject(it) }.orElse(null))
            } catch (e: JSONException) {
                Timber.e(e, "Error building JSON")
            }
        }
    }.toString()

    @JvmStatic
    fun fromString(job: Job, jsonString: String?): ResponseMap {
        val map = ImmutableMap.builder<String, Response>()
        if (jsonString == null) {
            return ResponseMap()
        }
        try {
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                try {
                    val taskId = keys.next()
                    val task = job.getTask(taskId)
                        .orElseThrow { LocalDataConsistencyException("Unknown task id $taskId") }
                    ResponseJsonConverter.toResponse(task, jsonObject[taskId])
                        .ifPresent { map.put(taskId, it) }
                } catch (e: LocalDataConsistencyException) {
                    Timber.d("Bad response in local db: ${e.message}")
                }
            }
        } catch (e: JSONException) {
            Timber.e(e, "Error parsing JSON string")
        }
        return ResponseMap(map.build())
    }
}