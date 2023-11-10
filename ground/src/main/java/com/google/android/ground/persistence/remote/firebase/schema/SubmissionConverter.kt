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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.submission.DateResponse
import com.google.android.ground.model.submission.GeometryTaskResponse
import com.google.android.ground.model.submission.MultipleChoiceResponse
import com.google.android.ground.model.submission.NumberResponse
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.SubmissionData
import com.google.android.ground.model.submission.TextResponse
import com.google.android.ground.model.submission.TimeResponse
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java8.util.Objects
import kotlinx.collections.immutable.toPersistentMap
import timber.log.Timber

/** Converts between Firestore documents and [Submission] instances. */
internal object SubmissionConverter {

  fun toSubmission(loi: LocationOfInterest, snapshot: DocumentSnapshot): Submission {
    if (!snapshot.exists()) throw DataStoreException("Missing submission")
    val doc = snapshot.toObject(SubmissionDocument::class.java)
    val loiId = DataStoreException.checkNotNull(doc!!.loiId, "loiId")
    if (loi.id != loiId) {
      throw DataStoreException("Submission doc featureId doesn't match specified loiId")
    }
    // Degrade gracefully when audit info missing in remote db.
    val created = Objects.requireNonNullElse(doc.created, AuditInfoNestedObject.FALLBACK_VALUE)
    val lastModified = Objects.requireNonNullElse(doc.lastModified, created)
    val job = loi.job
    return Submission(
      snapshot.id,
      loi.surveyId,
      loi,
      job,
      AuditInfoConverter.toAuditInfo(created!!),
      AuditInfoConverter.toAuditInfo(lastModified!!),
      // TODO(#2058): Remove reference to `responses` once dev dbs updated or reset.
      toSubmissionDataMap(snapshot.id, job, doc.data ?: doc.responses)
    )
  }

  private fun toSubmissionDataMap(
    submissionId: String,
    job: Job,
    docResponses: Map<String, Any>?
  ): SubmissionData {
    if (docResponses == null) {
      return SubmissionData()
    }
    val responses = mutableMapOf<String, Value>()
    for ((taskId, value) in docResponses) {
      try {
        putResponse(taskId, job, value, responses)
      } catch (e: DataStoreException) {
        Timber.e(e, "Task $taskId in remote db in submission $submissionId")
      }
    }
    return SubmissionData(responses.toPersistentMap())
  }

  private fun putResponse(
    taskId: String,
    job: Job,
    obj: Any,
    responses: MutableMap<String, Value>
  ) {
    val task = job.getTask(taskId)
    when (task.type) {
      Task.Type.PHOTO,
      Task.Type.TEXT -> putTextResponse(taskId, obj, responses)
      Task.Type.MULTIPLE_CHOICE ->
        putMultipleChoiceResponse(taskId, task.multipleChoice, obj, responses)
      Task.Type.NUMBER -> putNumberResponse(taskId, obj, responses)
      Task.Type.DATE -> putDateResponse(taskId, obj, responses)
      Task.Type.TIME -> putTimeResponse(taskId, obj, responses)
      Task.Type.DROP_A_PIN -> putDropAPinResponse(taskId, obj, responses)
      Task.Type.DRAW_POLYGON -> putDrawPolygonResponse(taskId, obj, responses)
      Task.Type.CAPTURE_LOCATION -> putCaptureLocationResponse(taskId, obj, responses)
      else -> throw DataStoreException("Unknown type " + task.type)
    }
  }

  private fun putNumberResponse(taskId: String, obj: Any, responses: MutableMap<String, Value>) {
    val value = DataStoreException.checkType(Double::class.java, obj) as Double
    NumberResponse.fromNumber(value.toString())?.let { r: Value -> responses[taskId] = r }
  }

  private fun putTextResponse(taskId: String, obj: Any, responses: MutableMap<String, Value>) {
    val value = DataStoreException.checkType(String::class.java, obj) as String
    TextResponse.fromString(value.trim { it <= ' ' })?.let { r: Value -> responses[taskId] = r }
  }

  private fun putDateResponse(taskId: String, obj: Any, responses: MutableMap<String, Value>) {
    val value = DataStoreException.checkType(Timestamp::class.java, obj) as Timestamp
    DateResponse.fromDate(value.toDate())?.let { r: Value -> responses[taskId] = r }
  }

  private fun putTimeResponse(taskId: String, obj: Any, responses: MutableMap<String, Value>) {
    val value = DataStoreException.checkType(Timestamp::class.java, obj) as Timestamp
    TimeResponse.fromDate(value.toDate())?.let { r: Value -> responses[taskId] = r }
  }

  private fun putDropAPinResponse(taskId: String, obj: Any, responses: MutableMap<String, Value>) {
    val map = obj as HashMap<String, *>
    check(map["type"] == "Point")
    val result = GeometryConverter.fromFirestoreMap(map).getOrNull()
    if (result != null) {
      responses[taskId] = GeometryTaskResponse(result)
    }
  }

  private fun putDrawPolygonResponse(
    taskId: String,
    obj: Any,
    responses: MutableMap<String, Value>
  ) {
    val map = obj as HashMap<String, *>
    check(map["type"] == "Polygon")
    val result = GeometryConverter.fromFirestoreMap(map).getOrNull()
    if (result != null) {
      responses[taskId] = GeometryTaskResponse(result)
    }
  }

  private fun putCaptureLocationResponse(
    taskId: String,
    obj: Any,
    responses: MutableMap<String, Value>
  ) =
    CaptureLocationResultConverter.fromFirestoreMap(obj as Map<String, *>).onSuccess {
      responses[taskId] = it
    }

  private fun putMultipleChoiceResponse(
    taskId: String,
    multipleChoice: MultipleChoice?,
    obj: Any,
    responses: MutableMap<String, Value>
  ) {
    val values = DataStoreException.checkType(MutableList::class.java, obj) as List<*>
    values.forEach { DataStoreException.checkType(String::class.java, it as Any) }
    MultipleChoiceResponse.fromList(multipleChoice, values as List<String>)?.let {
      responses[taskId] = it
    }
  }
}
