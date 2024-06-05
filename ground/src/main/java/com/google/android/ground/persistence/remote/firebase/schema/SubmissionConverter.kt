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

import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.submission.DateTaskData
import com.google.android.ground.model.submission.DrawAreaTaskData
import com.google.android.ground.model.submission.DropPinTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.SubmissionData
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.TimeTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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
    val created = doc.created ?: AuditInfoNestedObject.FALLBACK_VALUE
    val lastModified = doc.lastModified ?: created
    val job = loi.job
    return Submission(
      snapshot.id,
      loi.surveyId,
      loi,
      job,
      AuditInfoConverter.toAuditInfo(created),
      AuditInfoConverter.toAuditInfo(lastModified),
      // TODO(#2058): Remove reference to `responses` once dev dbs updated or reset.
      toSubmissionData(snapshot.id, job, doc.data ?: doc.responses),
    )
  }

  private fun toSubmissionData(
    submissionId: String,
    job: Job,
    firestoreMap: Map<String, Any>?,
  ): SubmissionData {
    if (firestoreMap == null) {
      return SubmissionData()
    }
    val data = mutableMapOf<String, TaskData>()
    for ((taskId, value) in firestoreMap) {
      try {
        putValue(taskId, job, value, data)
      } catch (e: DataStoreException) {
        Timber.e(e, "Task $taskId in remote db in submission $submissionId")
      }
    }
    return SubmissionData(data.toPersistentMap())
  }

  private fun putValue(taskId: String, job: Job, obj: Any, data: MutableMap<String, TaskData>) {
    try {
      val task = job.getTask(taskId)
      when (task.type) {
        Task.Type.PHOTO,
        Task.Type.TEXT -> putTextResponse(taskId, obj, data)
        Task.Type.MULTIPLE_CHOICE ->
          putMultipleChoiceResponse(taskId, task.multipleChoice, obj, data)
        Task.Type.NUMBER -> putNumberResponse(taskId, obj, data)
        Task.Type.DATE -> putDateResponse(taskId, obj, data)
        Task.Type.TIME -> putTimeResponse(taskId, obj, data)
        Task.Type.DROP_PIN -> putDropPinTaskResult(taskId, obj, data)
        Task.Type.DRAW_AREA -> putDrawAreaTaskResult(taskId, obj, data)
        Task.Type.CAPTURE_LOCATION -> putCaptureLocationResult(taskId, obj, data)
        else -> throw DataStoreException("Unknown type " + task.type)
      }
    } catch (e: Job.TaskNotFoundException) {
      Timber.d(e, "cannot put value for unknown task")
    }
  }

  private fun putNumberResponse(taskId: String, obj: Any, data: MutableMap<String, TaskData>) {
    val value = DataStoreException.checkType(Double::class.java, obj) as Double
    NumberTaskData.fromNumber(value.toString())?.let { r: TaskData -> data[taskId] = r }
  }

  private fun putTextResponse(taskId: String, obj: Any, data: MutableMap<String, TaskData>) {
    val value = DataStoreException.checkType(String::class.java, obj) as String
    TextTaskData.fromString(value.trim { it <= ' ' })?.let { r: TaskData -> data[taskId] = r }
  }

  private fun putDateResponse(taskId: String, obj: Any, data: MutableMap<String, TaskData>) {
    val value = DataStoreException.checkType(Timestamp::class.java, obj) as Timestamp
    DateTaskData.fromDate(value.toDate())?.let { r: TaskData -> data[taskId] = r }
  }

  private fun putTimeResponse(taskId: String, obj: Any, data: MutableMap<String, TaskData>) {
    val value = DataStoreException.checkType(Timestamp::class.java, obj) as Timestamp
    TimeTaskData.fromDate(value.toDate())?.let { r: TaskData -> data[taskId] = r }
  }

  private fun putDropPinTaskResult(taskId: String, obj: Any, data: MutableMap<String, TaskData>) {
    val map = obj as HashMap<String, *>
    check(map["type"] == "Point")
    val geometry = GeometryConverter.fromFirestoreMap(map).getOrNull()
    DataStoreException.checkNotNull(geometry, "Drop pin geometry null in remote db")
    DataStoreException.checkType(Point::class.java, geometry!!)
    data[taskId] = DropPinTaskData(geometry as Point)
  }

  private fun putDrawAreaTaskResult(taskId: String, obj: Any, data: MutableMap<String, TaskData>) {
    val map = obj as HashMap<String, *>
    check(map["type"] == "Polygon")
    val geometry = GeometryConverter.fromFirestoreMap(map).getOrNull()
    DataStoreException.checkNotNull(geometry, "Drop pin geometry null in remote db")
    DataStoreException.checkType(Polygon::class.java, geometry!!)
    data[taskId] = DrawAreaTaskData(geometry as Polygon)
  }

  private fun putCaptureLocationResult(
    taskId: String,
    obj: Any,
    data: MutableMap<String, TaskData>,
  ) =
    CaptureLocationResultConverter.fromFirestoreMap(obj as Map<String, *>).onSuccess {
      data[taskId] = it
    }

  private fun putMultipleChoiceResponse(
    taskId: String,
    multipleChoice: MultipleChoice?,
    obj: Any,
    data: MutableMap<String, TaskData>,
  ) {
    val values = DataStoreException.checkType(MutableList::class.java, obj) as List<*>
    values.forEach { DataStoreException.checkType(String::class.java, it as Any) }
    MultipleChoiceTaskData.fromList(multipleChoice, values as List<String>)?.let {
      data[taskId] = it
    }
  }
}
