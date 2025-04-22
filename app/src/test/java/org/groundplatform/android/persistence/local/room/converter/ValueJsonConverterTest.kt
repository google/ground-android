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
package org.groundplatform.android.persistence.local.room.converter

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.collections.immutable.persistentListOf
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.submission.DateTimeTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskData
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.MultipleChoiceTaskData
import org.groundplatform.android.model.submission.NumberTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.model.task.Task
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
