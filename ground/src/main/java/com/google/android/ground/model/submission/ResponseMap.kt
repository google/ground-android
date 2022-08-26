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
 * An immutable map of task ids to related user responses.
 *
 * @property responses A map from task id to user response. This map is mutable and therefore should never be exposed
 * outside this class.
 */
data class ResponseMap constructor(private val responses: Map<String, Response?> = ImmutableMap.of()) {

    /**
     * Returns the user response for the given task id, or empty if the user did not specify a
     * response.
     */
    fun getResponse(taskId: String): Optional<Response> {
        return Optional.ofNullable(
            responses[taskId]
        )
    }

    /**
     * Returns an Iterable over the task ids in this map.
     */
    fun taskIds(): Iterable<String> {
        return responses.keys
    }

    /**
     * Adds, replaces, and/or removes responses based on the provided list of deltas.
     */
    fun copyWithDeltas(responseDeltas: ImmutableList<ResponseDelta>): ResponseMap {
        val newResponses = responses.toMutableMap()
        responseDeltas.forEach {
            if (it.newResponse.isPresent) {
                newResponses[it.taskId] = it.newResponse.get()
            } else {
                newResponses.remove(it.taskId)
            }
        }

        return ResponseMap(newResponses)
    }
}