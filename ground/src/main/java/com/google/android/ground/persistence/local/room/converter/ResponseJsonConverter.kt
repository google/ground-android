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

import com.google.android.ground.model.submission.DateTaskData
import com.google.android.ground.model.submission.GeometryData
import com.google.android.ground.model.submission.LocationTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.TimeTaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.DataStoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.collections.immutable.toPersistentList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

internal object ResponseJsonConverter {

  private const val ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mmZ"

  fun toJsonObject(taskData: TaskData?): Any {
    if (taskData == null) return JSONObject.NULL
    return when (taskData) {
      is TextTaskData -> taskData.text
      is MultipleChoiceTaskData -> toJsonArray(taskData)
      is NumberTaskData -> taskData.value
      is DateTaskData -> dateToIsoString(taskData.date)
      is TimeTaskData -> dateToIsoString(taskData.time)
      is GeometryData -> GeometryWrapperTypeConverter.toString(taskData.geometry)
      is LocationTaskData ->
        JSONObject().apply {
          put("accuracy", taskData.accuracy)
          put("altitude", taskData.altitude)
          put("geometry", GeometryWrapperTypeConverter.toString(taskData.geometry))
        }
      else -> throw UnsupportedOperationException("Unimplemented taskData ${taskData.javaClass}")
    }
  }

  private fun dateToIsoString(date: Date): String =
    SimpleDateFormat(ISO_DATE_TIME_FORMAT, Locale.getDefault())
      .apply { timeZone = TimeZone.getTimeZone("UTC") }
      .format(date)

  private fun isoStringToDate(isoString: String): Date? =
    SimpleDateFormat(ISO_DATE_TIME_FORMAT, Locale.getDefault())
      .apply { timeZone = TimeZone.getTimeZone("UTC") }
      .parse(isoString)

  private fun toJsonArray(response: MultipleChoiceTaskData): JSONArray =
    JSONArray().apply { response.selectedOptionIds.forEach { this.put(it) } }

  fun toResponse(task: Task, obj: Any): TaskData? {
    if (JSONObject.NULL === obj) {
      return null
    }
    return when (task.type) {
      Task.Type.TEXT,
      Task.Type.PHOTO -> {
        DataStoreException.checkType(String::class.java, obj)
        TextTaskData.fromString(obj as String)
      }
      Task.Type.MULTIPLE_CHOICE -> {
        DataStoreException.checkType(JSONArray::class.java, obj)
        MultipleChoiceTaskData.fromList(task.multipleChoice, toList(obj as JSONArray))
      }
      Task.Type.NUMBER -> {
        DataStoreException.checkType(Number::class.java, obj)
        NumberTaskData.fromNumber(obj.toString())
      }
      Task.Type.DATE -> {
        DataStoreException.checkType(String::class.java, obj)
        DateTaskData.fromDate(isoStringToDate(obj as String))
      }
      Task.Type.TIME -> {
        DataStoreException.checkType(String::class.java, obj)
        TimeTaskData.fromDate(isoStringToDate(obj as String))
      }
      Task.Type.DRAW_POLYGON,
      Task.Type.DROP_A_PIN -> {
        GeometryData.fromGeometry(
          GeometryWrapperTypeConverter.fromString(obj as String)?.getGeometry()
        )
      }
      Task.Type.CAPTURE_LOCATION -> {
        DataStoreException.checkType(JSONObject::class.java, obj)
        val data = obj as JSONObject
        val accuracy = data.getDouble("accuracy")
        val altitude = data.getDouble("altitude")
        val geometry =
          GeometryWrapperTypeConverter.fromString(data.getString("geometry"))?.getGeometry()
        LocationTaskData(geometry, accuracy, altitude)
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
        Timber.e("Error parsing JSONArray in db: %s", jsonArray)
      }
    }
    return list.toPersistentList()
  }
}
