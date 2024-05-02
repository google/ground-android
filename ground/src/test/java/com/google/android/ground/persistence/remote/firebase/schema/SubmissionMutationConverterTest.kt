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
package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.DrawAreaTaskData
import com.google.android.ground.model.submission.DropPinTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.GeoPoint
import com.sharedtest.FakeData
import java.util.Date
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

// TODO(#1734): Add coverage for date, time task types
@RunWith(MockitoJUnitRunner::class)
class SubmissionMutationConverterTest {

  private val user = FakeData.USER.copy(id = "user_id_1")
  private val job = FakeData.JOB
  private val loiId = "loi_id_1"
  private val clientTimestamp = Date()

  private val textTaskData = TextTaskData.fromString("some data")

  private val singleChoiceResponse =
    MultipleChoiceTaskData.fromList(
      MultipleChoice(
        persistentListOf(
          Option("option id 1", "code1", "Option 1"),
          Option("option id 2", "code2", "Option 2"),
        ),
        MultipleChoice.Cardinality.SELECT_ONE,
      ),
      ids = listOf("option id 1"),
    )

  private val multipleChoiceTaskData =
    MultipleChoiceTaskData.fromList(
      MultipleChoice(
        persistentListOf(
          Option("option id 1", "code1", "Option 1"),
          Option("option id 2", "code2", "Option 2"),
        ),
        MultipleChoice.Cardinality.SELECT_MULTIPLE,
      ),
      ids = listOf("option id 1", "option id 2"),
    )

  private val numberTaskData = NumberTaskData.fromNumber("123")

  private val dropPinTaskResult = DropPinTaskData(Point(Coordinates(10.0, 20.0)))

  private val drawAreaTaskResult =
    DrawAreaTaskData(
      Polygon(
        LinearRing(
          listOf(
            Coordinates(10.0, 20.0),
            Coordinates(20.0, 30.0),
            Coordinates(30.0, 20.0),
            Coordinates(10.0, 20.0),
          )
        )
      )
    )

  private val submissionMutation =
    SubmissionMutation(
      id = 1,
      surveyId = "id_1",
      locationOfInterestId = loiId,
      userId = user.id,
      clientTimestamp = clientTimestamp,
      job = job,
      deltas =
        listOf(
          ValueDelta(taskId = "text_task", taskType = Task.Type.TEXT, newTaskData = textTaskData),
          ValueDelta(
            taskId = "single_choice_task",
            taskType = Task.Type.MULTIPLE_CHOICE,
            newTaskData = singleChoiceResponse,
          ),
          ValueDelta(
            taskId = "multiple_choice_task",
            taskType = Task.Type.MULTIPLE_CHOICE,
            newTaskData = multipleChoiceTaskData,
          ),
          ValueDelta(
            taskId = "number_task",
            taskType = Task.Type.NUMBER,
            newTaskData = numberTaskData,
          ),
          ValueDelta(
            taskId = "drop_pin_task",
            taskType = Task.Type.DROP_PIN,
            newTaskData = dropPinTaskResult,
          ),
          ValueDelta(
            taskId = "draw_area_task",
            taskType = Task.Type.DRAW_AREA,
            newTaskData = drawAreaTaskResult,
          ),
        ),
    )

  private val expected =
    mapOf(
      Pair("text_task", "some data"),
      Pair("single_choice_task", listOf("option id 1")),
      Pair("multiple_choice_task", listOf("option id 1", "option id 2")),
      Pair("number_task", 123.0),
      Pair(
        "drop_pin_task",
        mapOf(Pair("type", "Point"), Pair("coordinates", GeoPoint(10.0, 20.0))),
      ),
      Pair(
        "draw_area_task",
        mapOf(
          Pair("type", "Polygon"),
          Pair(
            "coordinates",
            mapOf(
              Pair(
                "0",
                mapOf(
                  Pair("0", GeoPoint(10.0, 20.0)),
                  Pair("1", GeoPoint(20.0, 30.0)),
                  Pair("2", GeoPoint(30.0, 20.0)),
                  Pair("3", GeoPoint(10.0, 20.0)),
                ),
              )
            ),
          ),
        ),
      ),
    )

  private val auditInfoObject = AuditInfoConverter.fromMutationAndUser(submissionMutation, user)

  @Test
  fun testToMap_create() {
    assertThat(
        SubmissionMutationConverter.toMap(
          submissionMutation.copy(type = Mutation.Type.CREATE),
          user,
        )
      )
      .isEqualTo(
        mapOf(
          Pair("created", auditInfoObject),
          Pair("lastModified", auditInfoObject),
          Pair("loiId", loiId),
          Pair("jobId", job.id),
          Pair("data", expected),
        )
      )
  }

  @Test
  fun testToMap_update() {
    assertThat(
        SubmissionMutationConverter.toMap(
          submissionMutation.copy(type = Mutation.Type.UPDATE),
          user,
        )
      )
      .isEqualTo(
        mapOf(
          Pair("lastModified", auditInfoObject),
          Pair("loiId", loiId),
          Pair("jobId", job.id),
          Pair("data", expected),
        )
      )
  }

  @Test
  fun testToMap_delete() {
    assertThrows("Unsupported mutation type", DataStoreException::class.java) {
      SubmissionMutationConverter.toMap(submissionMutation.copy(type = Mutation.Type.DELETE), user)
    }
  }

  @Test
  fun testToMap_unknown() {
    assertThrows("Unsupported mutation type", DataStoreException::class.java) {
      SubmissionMutationConverter.toMap(submissionMutation.copy(type = Mutation.Type.UNKNOWN), user)
    }
  }
}
