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

import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.submission.DateTaskData
import com.google.android.ground.model.submission.GeometryData
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.TimeTaskData
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
  private val taskData: TaskData,
  private val responseObject: Any
) {

  @Test
  fun testToJsonObject() {
    assertThat(ResponseJsonConverter.toJsonObject(taskData)).isEqualTo(responseObject)
  }

  @Test
  fun testToResponse() {
    assertThat(ResponseJsonConverter.toResponse(task, responseObject)).isEqualTo(taskData)
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

    private val singleChoiceTaskData =
      MultipleChoiceTaskData.fromList(
        MultipleChoice(multipleChoiceOptions, MultipleChoice.Cardinality.SELECT_ONE),
        listOf("option id 1")
      )

    private val singleChoiceTaskDataResponse = JSONArray().apply { put("option id 1") }

    private val multipleChoiceTaskData =
      MultipleChoiceTaskData.fromList(
        MultipleChoice(multipleChoiceOptions, MultipleChoice.Cardinality.SELECT_MULTIPLE),
        listOf("option id 1", "option id 2")
      )

    private val multipleChoiceTaskDataResponse =
      JSONArray().apply {
        put("option id 1")
        put("option id 2")
      }

    private val pointGeometryTaskData = GeometryData.fromGeometry(Point(Coordinate(10.0, 20.0)))

    private const val pointGeometryTaskDataResponse = "HQoFcG9pbnQSFAoSCQAAAAAAACRAEQAAAAAAADRA\n"

    private val polygonGeometryTaskData =
      GeometryData.fromGeometry(
        Polygon(
          LinearRing(
            listOf(
              Coordinate(10.0, 20.0),
              Coordinate(20.0, 30.0),
              Coordinate(30.0, 40.0),
              Coordinate(10.0, 20.0)
            )
          )
        )
      )

    private const val polygonGeometryTaskDataResponse =
      "XQoHcG9seWdvbhJSClAKEgkAAAAAAAAkQBEAAAAAAAA0QAoSCQAAAAAAADRAEQAAAAAAAD5AChIJ\n" +
        "AAAAAAAAPkARAAAAAAAAREAKEgkAAAAAAAAkQBEAAAAAAAA0QA==\n"

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "task:{0}, data:{1}, jsonObject:{2}")
    fun data() =
      listOf(
        arrayOf(
          FakeData.newTask(type = Task.Type.TEXT),
          TextTaskData.fromString("sample text"),
          "sample text"
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.MULTIPLE_CHOICE),
          singleChoiceTaskData,
          singleChoiceTaskDataResponse
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.MULTIPLE_CHOICE),
          multipleChoiceTaskData,
          multipleChoiceTaskDataResponse
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.NUMBER),
          NumberTaskData.fromNumber("12345.0"),
          12345.0
        ),
        arrayOf(FakeData.newTask(type = Task.Type.DATE), DateTaskData.fromDate(DATE), DATE_STRING),
        arrayOf(FakeData.newTask(type = Task.Type.TIME), TimeTaskData.fromDate(DATE), DATE_STRING),
        arrayOf(
          FakeData.newTask(type = Task.Type.DROP_A_PIN),
          pointGeometryTaskData,
          pointGeometryTaskDataResponse
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.DRAW_POLYGON),
          polygonGeometryTaskData,
          polygonGeometryTaskDataResponse
        ),
      )
  }
}
