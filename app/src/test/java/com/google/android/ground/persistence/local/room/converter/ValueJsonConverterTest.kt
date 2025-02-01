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

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.submission.DateTimeTaskData
import com.google.android.ground.model.submission.DrawAreaTaskData
import com.google.android.ground.model.submission.DropPinTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.collections.immutable.persistentListOf
import org.json.JSONArray
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@HiltAndroidTest
@RunWith(ParameterizedRobolectricTestRunner::class)
class ValueJsonConverterTest(
  private val task: Task,
  private val taskData: TaskData,
  private val input: Any,
) : BaseHiltTest() {

  @Test
  fun testToJsonObject() {
    assertThat(ValueJsonConverter.toJsonObject(taskData)).isEqualTo(input)
  }

  @Test
  fun testToResponse() {
    assertThat(ValueJsonConverter.toResponse(task, input)).isEqualTo(taskData)
  }

  companion object {
    private const val dateTimeResponse = 1725537603066

    private val dateTimeOption = DateTimeTaskData.fromMillis(1725537603066)

    private val multipleChoiceOptions =
      persistentListOf(
        Option("option id 1", "code1", "Option 1"),
        Option("option id 2", "code2", "Option 2"),
      )

    private val singleChoiceResponse =
      MultipleChoiceTaskData.fromList(
        MultipleChoice(multipleChoiceOptions, MultipleChoice.Cardinality.SELECT_ONE),
        listOf("option id 1"),
      )

    private val singleChoiceResponseJson = JSONArray().apply { put("option id 1") }

    private val multipleChoiceTaskData =
      MultipleChoiceTaskData.fromList(
        MultipleChoice(multipleChoiceOptions, MultipleChoice.Cardinality.SELECT_MULTIPLE),
        listOf("option id 1", "option id 2"),
      )

    private val multipleChoiceResponseJson =
      JSONArray().apply {
        put("option id 1")
        put("option id 2")
      }

    private val dropPinTaskResponse = DropPinTaskData(Point(Coordinates(10.0, 20.0)))

    private const val dropPinGeometryTaskResponseString =
      "HQoFcG9pbnQSFAoSCQAAAAAAACRAEQAAAAAAADRA\n"

    private val drawAreaTaskResponse =
      DrawAreaTaskData(
        Polygon(
          LinearRing(
            listOf(
              Coordinates(10.0, 20.0),
              Coordinates(20.0, 30.0),
              Coordinates(30.0, 40.0),
              Coordinates(10.0, 20.0),
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
          TextTaskData.fromString("sample text"),
          "sample text",
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.MULTIPLE_CHOICE),
          singleChoiceResponse,
          singleChoiceResponseJson,
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.MULTIPLE_CHOICE),
          multipleChoiceTaskData,
          multipleChoiceResponseJson,
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.NUMBER),
          NumberTaskData.fromNumber("12345.0"),
          12345.0,
        ),
        arrayOf(FakeData.newTask(type = Task.Type.DATE), dateTimeOption, dateTimeResponse),
        arrayOf(FakeData.newTask(type = Task.Type.TIME), dateTimeOption, dateTimeResponse),
        arrayOf(
          FakeData.newTask(type = Task.Type.DROP_PIN),
          dropPinTaskResponse,
          dropPinGeometryTaskResponseString,
        ),
        arrayOf(
          FakeData.newTask(type = Task.Type.DRAW_AREA),
          drawAreaTaskResponse,
          polygonGeometryTaskResponseString,
        ),
      )
  }
}
