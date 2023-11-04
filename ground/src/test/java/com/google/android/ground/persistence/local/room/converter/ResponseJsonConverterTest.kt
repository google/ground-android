/*
 * Copyright 2023 Google LLC
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

import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.submission.DateResponse
import com.google.android.ground.model.submission.GeometryTaskResponse
import com.google.android.ground.model.submission.MultipleChoiceResponse
import com.google.android.ground.model.submission.NumberResponse
import com.google.android.ground.model.submission.TextResponse
import com.google.android.ground.model.submission.TimeResponse
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import java.util.Date
import kotlinx.collections.immutable.persistentListOf
import org.json.JSONArray
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class ResponseJsonConverterTest(
  private val task: Task,
  private val value: Value,
  private val responseObject: Any
) {

  @Test
  fun testToJsonObject() {
    assertThat(ResponseJsonConverter.toJsonObject(value)).isEqualTo(responseObject)
  }

  @Test
  fun testToResponse() {
    assertThat(ResponseJsonConverter.toResponse(task, responseObject)).isEqualTo(value)
  }

  companion object {
    // Date represented in YYYY-MM-DDTHH:mmZ Format from 1632501600000L milliseconds.
    private const val DATE_STRING = "2021-09-21T07:00+0000"

    // Date represented in milliseconds for date: 2021-09-24T16:40+0000.
    private val DATE = Date(1632207600000L)

    private val multipleChoiceOptions =
      persistentListOf(
        Option("option id 1", "code1", "Option 1"),
        Option("option id 2", "code2", "Option 2"),
      )

    private val singleChoiceResponse =
      MultipleChoiceResponse.fromList(
        MultipleChoice(multipleChoiceOptions, MultipleChoice.Cardinality.SELECT_ONE),
        listOf("option id 1")
      )

    private val singleChoiceResponseJson = JSONArray().apply { put("option id 1") }

    private val multipleChoiceResponse =
      MultipleChoiceResponse.fromList(
        MultipleChoice(multipleChoiceOptions, MultipleChoice.Cardinality.SELECT_MULTIPLE),
        listOf("option id 1", "option id 2")
      )

    private val multipleChoiceResponseJson =
      JSONArray().apply {
        put("option id 1")
        put("option id 2")
      }

    private val pointGeometryTaskResponse =
      GeometryTaskResponse.fromGeometry(Point(Coordinates(10.0, 20.0)))

    private const val pointGeometryTaskResponseString = "HQoFcG9pbnQSFAoSCQAAAAAAACRAEQAAAAAAADRA\n"

    private val polygonGeometryTaskResponse =
      GeometryTaskResponse.fromGeometry(
        Polygon(
          LinearRing(
            listOf(
              Coordinates(10.0, 20.0),
              Coordinates(20.0, 30.0),
              Coordinates(30.0, 40.0),
              Coordinates(10.0, 20.0)
            )
          )
        )
      )

    private const val polygonGeometryTaskResponseString =
      "XQoHcG9seWdvbhJSClAKEgkAAAAAAAAkQBEAAAAAAAA0QAoSCQAAAAAAADRAEQAAAAAAAD5AChIJ\n" +
        "AAAAAAAAPkARAAAAAAAAREAKEgkAAAAAAAAkQBEAAAAAAAA0QA==\n"

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "task:{0}, data:{1}, jsonObject:{2}")
    fun data() =
      listOf(
        arrayOf(
          FakeData.newTask(type = Task.Type.TEXT),
          TextResponse.fromString("sample text"),
          "sample text"
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.MULTIPLE_CHOICE),
          singleChoiceResponse,
          singleChoiceResponseJson
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.MULTIPLE_CHOICE),
          multipleChoiceResponse,
          multipleChoiceResponseJson
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.NUMBER),
          NumberResponse.fromNumber("12345.0"),
          12345.0
        ),
        arrayOf(FakeData.newTask(type = Task.Type.DATE), DateResponse.fromDate(DATE), DATE_STRING),
        arrayOf(FakeData.newTask(type = Task.Type.TIME), TimeResponse.fromDate(DATE), DATE_STRING),
        arrayOf(
          FakeData.newTask(type = Task.Type.DROP_A_PIN),
          pointGeometryTaskResponse,
          pointGeometryTaskResponseString
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.DRAW_POLYGON),
          polygonGeometryTaskResponse,
          polygonGeometryTaskResponseString
        ),
      )
  }
}
