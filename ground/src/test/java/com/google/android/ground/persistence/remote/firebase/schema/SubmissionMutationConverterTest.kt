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
import com.google.android.ground.model.submission.CaptureLocationTaskData
import com.google.android.ground.model.submission.DateTaskData
import com.google.android.ground.model.submission.DrawAreaTaskData
import com.google.android.ground.model.submission.DropPinTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.TimeTaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.GeoPoint
import com.sharedtest.FakeData
import java.time.Instant
import java.util.Date
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

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

  private val captureLocationTaskResult =
    CaptureLocationTaskData(
      location = Point(Coordinates(10.0, 20.0)),
      accuracy = 80.8,
      altitude = 112.31,
    )

  private val dateTaskResult = DateTaskData(Date.from(Instant.EPOCH))

  private val timeTaskResult = TimeTaskData(Date.from(Instant.EPOCH))

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
          ValueDelta(
            taskId = "capture_location",
            taskType = Task.Type.CAPTURE_LOCATION,
            newTaskData = captureLocationTaskResult,
          ),
          ValueDelta(taskId = "date_task", taskType = Task.Type.DATE, newTaskData = dateTaskResult),
          ValueDelta(taskId = "time_task", taskType = Task.Type.TIME, newTaskData = timeTaskResult),
        ),
    )

  private val expected =
    mapOf(
      "text_task" to "some data",
      "single_choice_task" to listOf("option id 1"),
      "multiple_choice_task" to listOf("option id 1", "option id 2"),
      "number_task" to 123.0,
      "drop_pin_task" to mapOf("type" to "Point", "coordinates" to GeoPoint(10.0, 20.0)),
      "draw_area_task" to
        mapOf(
          "type" to "Polygon",
          "coordinates" to
            mapOf(
              "0" to
                mapOf(
                  "0" to GeoPoint(10.0, 20.0),
                  "1" to GeoPoint(20.0, 30.0),
                  "2" to GeoPoint(30.0, 20.0),
                  "3" to GeoPoint(10.0, 20.0),
                )
            ),
        ),
      "capture_location" to
        mapOf(
          "accuracy" to 80.8,
          "altitude" to 112.31,
          "geometry" to mapOf("type" to "Point", "coordinates" to GeoPoint(10.0, 20.0)),
        ),
      "date_task" to Date.from(Instant.EPOCH),
      "time_task" to Date.from(Instant.EPOCH),
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
          "created" to auditInfoObject,
          "lastModified" to auditInfoObject,
          "loiId" to loiId,
          "jobId" to job.id,
          "data" to expected,
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
          "lastModified" to auditInfoObject,
          "loiId" to loiId,
          "jobId" to job.id,
          "data" to expected,
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
