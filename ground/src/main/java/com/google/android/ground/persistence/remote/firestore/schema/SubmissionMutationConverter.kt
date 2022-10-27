/*
 * Copyright 2020 Google LLC
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

package com.google.android.ground.persistence.remote.firestore.schema

import com.google.android.ground.model.User
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.*
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firestore.schema.AuditInfoConverter.fromMutationAndUser
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.firebase.firestore.FieldValue
import timber.log.Timber

/** Converts between Firestore maps used to merge updates and [SubmissionMutation] instances. */
internal object SubmissionMutationConverter {

  const val LOI_ID = "loiId"
  private const val JOB_ID = "jobId"
  private const val RESPONSES = "responses"
  private const val CREATED = "created"
  private const val LAST_MODIFIED = "lastModified"

  @Throws(DataStoreException::class)
  fun toMap(mutation: SubmissionMutation, user: User): ImmutableMap<String, Any> {
    val map = ImmutableMap.builder<String, Any>()
    val auditInfo = fromMutationAndUser(mutation, user)
    when (mutation.type) {
      Mutation.Type.CREATE -> {
        map.put(CREATED, auditInfo)
        map.put(LAST_MODIFIED, auditInfo)
      }
      Mutation.Type.UPDATE -> map.put(LAST_MODIFIED, auditInfo)
      Mutation.Type.DELETE,
      Mutation.Type.UNKNOWN ->
        throw DataStoreException("Unsupported mutation type: ${mutation.type}")
    }
    map.put(LOI_ID, mutation.locationOfInterestId)
    map.put(JOB_ID, mutation.job!!.id)
    map.put(RESPONSES, toMap(mutation.taskDataDeltas))
    return map.build()
  }

  private fun toMap(taskDataDeltas: ImmutableList<TaskDataDelta>): Map<String, Any> {
    val map = ImmutableMap.builder<String, Any>()
    for (delta in taskDataDeltas) {
      delta.newTaskData
        .map { obj: TaskData -> toObject(obj) }
        .orElse(FieldValue.delete())
        ?.let { map.put(delta.taskId, it) }
    }
    return map.build()
  }

  private fun toObject(taskData: TaskData): Any? =
    when (taskData) {
      is TextTaskData -> taskData.text
      is MultipleChoiceTaskData -> taskData.selectedOptionIds
      is NumberTaskData -> taskData.value
      is TimeTaskData -> taskData.time
      is DateTaskData -> taskData.date
      else -> {
        Timber.e("Unknown taskData type: %s", taskData.javaClass.name)
        null
      }
    }
}
