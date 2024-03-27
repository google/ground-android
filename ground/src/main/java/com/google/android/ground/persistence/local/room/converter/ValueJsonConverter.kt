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

import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.submission.DateResponse
import com.google.android.ground.model.submission.MultipleChoiceResponse
import com.google.android.ground.model.submission.NumberResponse
import com.google.android.ground.model.submission.PhotoResponse
import com.google.android.ground.model.submission.TextResponse
import com.google.android.ground.model.submission.TimeResponse
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firebase.schema.CaptureLocationResultConverter.ACCURACY_KEY
import com.google.android.ground.persistence.remote.firebase.schema.CaptureLocationResultConverter.ALTITUDE_KEY
import com.google.android.ground.persistence.remote.firebase.schema.CaptureLocationResultConverter.GEOMETRY_KEY
import com.google.android.ground.ui.datacollection.tasks.location.CaptureLocationTaskResult
import com.google.android.ground.ui.datacollection.tasks.point.DropPinTaskResult
import com.google.android.ground.ui.datacollection.tasks.polygon.DrawAreaTaskResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.collections.immutable.toPersistentList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

internal object ValueJsonConverter {

  private const val ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mmZ"

  fun toJsonObject(value: Value?): Any {
    if (value == null) return JSONObject.NULL
    return when (value) {
      is TextResponse -> value.text
      is MultipleChoiceResponse -> toJsonArray(value)
      is NumberResponse -> value.value
      is DateResponse -> dateToIsoString(value.date)
      is TimeResponse -> dateToIsoString(value.time)
      is PhotoResponse -> value.getDetailsText()
      is DrawAreaTaskResult -> GeometryWrapperTypeConverter.toString(value.geometry)
      is DropPinTaskResult -> GeometryWrapperTypeConverter.toString(value.geometry)
      is CaptureLocationTaskResult ->
        JSONObject().apply {
          put("accuracy", value.accuracy)
          put("altitude", value.altitude)
          put("geometry", GeometryWrapperTypeConverter.toString(value.geometry))
        }

      else -> throw UnsupportedOperationException("Unimplemented value class ${value.javaClass}")
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

  private fun toJsonArray(response: MultipleChoiceResponse): JSONArray =
    JSONArray().apply { response.selectedOptionIds.forEach { this.put(it) } }

  fun toResponse(task: Task, obj: Any): Value? {
    if (JSONObject.NULL === obj) {
      return null
    }
    return when (task.type) {
      Task.Type.TEXT,
      Task.Type.PHOTO -> {
        DataStoreException.checkType(String::class.java, obj)
        TextResponse.fromString(obj as String)
      }

      Task.Type.MULTIPLE_CHOICE -> {
        DataStoreException.checkType(JSONArray::class.java, obj)
        MultipleChoiceResponse.fromList(task.multipleChoice, toList(obj as JSONArray))
      }

      Task.Type.NUMBER -> {
        DataStoreException.checkType(Number::class.java, obj)
        NumberResponse.fromNumber(obj.toString())
      }

      Task.Type.DATE -> {
        DataStoreException.checkType(String::class.java, obj)
        DateResponse.fromDate(isoStringToDate(obj as String))
      }

      Task.Type.TIME -> {
        DataStoreException.checkType(String::class.java, obj)
        TimeResponse.fromDate(isoStringToDate(obj as String))
      }

      Task.Type.DRAW_AREA -> {
        DataStoreException.checkType(String::class.java, obj)
        val geometry = GeometryWrapperTypeConverter.fromString(obj as String)?.getGeometry()
        DataStoreException.checkNotNull(geometry, "Missing geometry in draw area task result")
        DataStoreException.checkType(Polygon::class.java, geometry!!)
        DrawAreaTaskResult(geometry as Polygon)
      }

      Task.Type.DROP_PIN -> {
        DataStoreException.checkType(String::class.java, obj)
        val geometry = GeometryWrapperTypeConverter.fromString(obj as String)?.getGeometry()
        DataStoreException.checkNotNull(geometry, "Missing geometry in drop pin task result")
        DataStoreException.checkType(Point::class.java, geometry!!)
        DropPinTaskResult(geometry as Point)
      }

      Task.Type.CAPTURE_LOCATION -> {
        DataStoreException.checkType(JSONObject::class.java, obj)
        captureLocationResultFromJsonObject(obj as JSONObject).getOrThrow()
      }

      Task.Type.UNKNOWN -> {
        throw DataStoreException("Unknown type in task: " + obj.javaClass.name)
      }
    }
  }

  private fun captureLocationResultFromJsonObject(
    data: JSONObject
  ): Result<CaptureLocationTaskResult> =
    Result.runCatching {
      val accuracy = data.getDouble(ACCURACY_KEY)
      val altitude = data.getDouble(ALTITUDE_KEY)
      val geometry =
        GeometryWrapperTypeConverter.fromString(data.getString(GEOMETRY_KEY))?.getGeometry()
      CaptureLocationTaskResult(geometry as Point, accuracy, altitude)
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
