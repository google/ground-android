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

package org.groundplatform.android.data.local.room.converter

import kotlinx.collections.immutable.toPersistentList
import org.groundplatform.android.data.remote.DataStoreException
import org.groundplatform.android.data.remote.firebase.schema.CaptureLocationResultConverter.toCaptureLocationTaskData
import org.groundplatform.android.data.remote.firebase.schema.CaptureLocationResultConverter.toJSONObject
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.model.submission.DateTimeTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskIncompleteData
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.MultipleChoiceTaskData
import org.groundplatform.android.model.submission.NumberTaskData
import org.groundplatform.android.model.submission.PhotoTaskData
import org.groundplatform.android.model.submission.SkippedTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.model.task.Task
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

internal object ValueJsonConverter {

  private const val SKIPPED_KEY = "skipped"

  fun toJsonObject(taskData: TaskData?): Any {
    if (taskData == null) return JSONObject.NULL
    return when (taskData) {
      is TextTaskData -> taskData.text
      is MultipleChoiceTaskData -> toJsonArray(taskData)
      is NumberTaskData -> taskData.value
      is DateTimeTaskData -> taskData.timeInMillis
      is PhotoTaskData -> taskData.remoteFilename
      is DrawAreaTaskData -> GeometryWrapperTypeConverter.toString(taskData.geometry)
      is DropPinTaskData -> GeometryWrapperTypeConverter.toString(taskData.geometry)
      is DrawAreaTaskIncompleteData -> GeometryWrapperTypeConverter.toString(taskData.geometry)
      is CaptureLocationTaskData -> taskData.toJSONObject()
      is SkippedTaskData -> JSONObject().put(SKIPPED_KEY, true)
      else -> throw UnsupportedOperationException("Unimplemented value class ${taskData.javaClass}")
    }
  }

  private fun toJsonArray(response: MultipleChoiceTaskData): JSONArray =
    JSONArray().apply { response.selectedOptionIds.forEach { this.put(it) } }

  fun toResponse(task: Task, obj: Any): TaskData? {
    if (JSONObject.NULL === obj) {
      return null
    }

    if (obj is JSONObject && obj.optBoolean(SKIPPED_KEY, false)) {
      return SkippedTaskData()
    }

    return when (task.type) {
      Task.Type.TEXT -> {
        DataStoreException.checkType(String::class.java, obj)
        TextTaskData.fromString(obj as String)
      }
      Task.Type.PHOTO -> {
        DataStoreException.checkType(String::class.java, obj)
        PhotoTaskData(obj as String)
      }
      Task.Type.MULTIPLE_CHOICE -> {
        DataStoreException.checkType(JSONArray::class.java, obj)
        MultipleChoiceTaskData.fromList(task.multipleChoice, toList(obj as JSONArray))
      }
      Task.Type.NUMBER -> {
        DataStoreException.checkType(Number::class.java, obj)
        NumberTaskData.fromNumber(obj.toString())
      }
      Task.Type.DATE,
      Task.Type.TIME -> {
        DataStoreException.checkType(Long::class.java, obj)
        DateTimeTaskData.fromMillis(obj as Long)
      }
      Task.Type.DRAW_AREA -> {
        DataStoreException.checkType(String::class.java, obj)
        val geometry = GeometryWrapperTypeConverter.fromString(obj as String)?.getGeometry()
        DataStoreException.checkNotNull(geometry, "Missing geometry in draw area task result")
        when (geometry) {
          is Polygon -> DrawAreaTaskData(geometry)
          is LineString -> DrawAreaTaskIncompleteData(geometry)
          else ->
            throw DataStoreException(
              "Unknown geometry type in draw area task result: ${geometry?.javaClass?.name}"
            )
        }
      }
      Task.Type.DROP_PIN -> {
        DataStoreException.checkType(String::class.java, obj)
        val geometry = GeometryWrapperTypeConverter.fromString(obj as String)?.getGeometry()
        DataStoreException.checkNotNull(geometry, "Missing geometry in drop pin task result")
        DataStoreException.checkType(Point::class.java, geometry!!)
        DropPinTaskData(geometry as Point)
      }
      Task.Type.CAPTURE_LOCATION -> {
        DataStoreException.checkType(JSONObject::class.java, obj)
        (obj as JSONObject).toCaptureLocationTaskData()
      }
      Task.Type.INSTRUCTIONS -> {
        null
      }
      Task.Type.UNKNOWN -> {
        throw DataStoreException("Unknown type in task: " + obj.javaClass.name)
      }
    }
  }

  private fun toList(jsonArray: JSONArray): List<String> {
    val list: MutableList<String> = ArrayList(jsonArray.length())
    for (i in 0 until jsonArray.length()) {
      try {
        list.add(jsonArray.getString(i))
      } catch (e: JSONException) {
        Timber.e(e, "Error parsing JSONArray in db: $jsonArray")
      }
    }
    return list.toPersistentList()
  }
}
