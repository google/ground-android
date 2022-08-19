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

import com.google.android.ground.model.submission.*
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.common.collect.ImmutableList
import java8.util.Optional
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

internal object ResponseJsonConverter {

    private val ISO_INSTANT_FORMAT: DateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.getDefault())

    @JvmStatic
    fun toJsonObject(response: Response): Any =
        when (response) {
            is TextResponse -> response.text
            is MultipleChoiceResponse -> toJsonArray(response)
            is NumberResponse -> response.value
            is DateResponse -> dateToIsoString(response.date)
            is TimeResponse -> dateToIsoString(response.time)
            else -> throw UnsupportedOperationException("Unimplemented response ${response.javaClass}")
        }

    @JvmStatic
    fun dateToIsoString(date: Date): String {
        synchronized(ISO_INSTANT_FORMAT) {
            ISO_INSTANT_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
            return ISO_INSTANT_FORMAT.format(date)
        }
    }

    @JvmStatic
    fun isoStringToDate(isoString: String): Date {
        synchronized(ISO_INSTANT_FORMAT) {
            ISO_INSTANT_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
            return ISO_INSTANT_FORMAT.parse(isoString)
        }
    }

    private fun toJsonArray(response: MultipleChoiceResponse): JSONArray =
        JSONArray().apply {
            response.selectedOptionIds.forEach { this.put(it) }
        }

    @JvmStatic
    fun toResponse(task: Task, obj: Any): Optional<Response> =
        when (task.type) {
            Task.Type.TEXT, Task.Type.PHOTO -> {
                if (obj === JSONObject.NULL) {
                    TextResponse.fromString("")
                } else {
                    DataStoreException.checkType(String::class.java, obj)
                    TextResponse.fromString(obj as String)
                }
            }
            Task.Type.MULTIPLE_CHOICE -> {
                if (obj === JSONObject.NULL) {
                    MultipleChoiceResponse.fromList(task.multipleChoice, emptyList())
                } else {
                    DataStoreException.checkType(JSONArray::class.java, obj)
                    MultipleChoiceResponse.fromList(task.multipleChoice, toList(obj as JSONArray))
                }
            }
            Task.Type.NUMBER -> {
                if (JSONObject.NULL === obj) {
                    NumberResponse.fromNumber("")
                } else {
                    DataStoreException.checkType(Number::class.java, obj)
                    NumberResponse.fromNumber(obj.toString())
                }
            }
            Task.Type.DATE -> {
                DataStoreException.checkType(String::class.java, obj)
                DateResponse.fromDate(isoStringToDate(obj as String))
            }
            Task.Type.TIME -> {
                DataStoreException.checkType(String::class.java, obj)
                TimeResponse.fromDate(isoStringToDate(obj as String))
            }
            Task.Type.UNKNOWN -> throw DataStoreException("Unknown type in task: " + obj.javaClass.name)
            else -> throw DataStoreException("Unknown type in task: " + obj.javaClass.name)
        }

    private fun toList(jsonArray: JSONArray): ImmutableList<String> {
        val list: MutableList<String> = ArrayList(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            try {
                list.add(jsonArray.getString(i))
            } catch (e: JSONException) {
                Timber.e("Error parsing JSONArray in db: %s", jsonArray)
            }
        }
        return ImmutableList.builder<String>().addAll(list).build()
    }
}
