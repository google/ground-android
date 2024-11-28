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
import com.google.android.ground.model.submission.DateTimeTaskData
import com.google.android.ground.model.submission.DrawAreaTaskData
import com.google.android.ground.model.submission.DropPinTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.submission.SkippedTaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.firebase.protobuf.createSubmissionMessage
import com.google.android.ground.persistence.remote.firebase.protobuf.toFirestoreMap
import com.google.android.ground.proto.AuditInfo.CLIENT_TIMESTAMP_FIELD_NUMBER
import com.google.android.ground.proto.AuditInfo.DISPLAY_NAME_FIELD_NUMBER
import com.google.android.ground.proto.AuditInfo.SERVER_TIMESTAMP_FIELD_NUMBER
import com.google.android.ground.proto.AuditInfo.USER_ID_FIELD_NUMBER
import com.google.android.ground.proto.Coordinates.LATITUDE_FIELD_NUMBER
import com.google.android.ground.proto.Coordinates.LONGITUDE_FIELD_NUMBER
import com.google.android.ground.proto.Geometry.POINT_FIELD_NUMBER
import com.google.android.ground.proto.Geometry.POLYGON_FIELD_NUMBER
import com.google.android.ground.proto.Point.COORDINATES_FIELD_NUMBER
import com.google.android.ground.proto.Polygon.SHELL_FIELD_NUMBER
import com.google.android.ground.proto.Submission.CREATED_FIELD_NUMBER
import com.google.android.ground.proto.Submission.ID_FIELD_NUMBER
import com.google.android.ground.proto.Submission.JOB_ID_FIELD_NUMBER
import com.google.android.ground.proto.Submission.LAST_MODIFIED_FIELD_NUMBER
import com.google.android.ground.proto.Submission.LOI_ID_FIELD_NUMBER
import com.google.android.ground.proto.Submission.OWNER_ID_FIELD_NUMBER
import com.google.android.ground.proto.Submission.TASK_DATA_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.CAPTURE_LOCATION_RESULT_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.CaptureLocationResult.ACCURACY_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.CaptureLocationResult.ALTITUDE_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.DATE_TIME_RESPONSE_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.DRAW_GEOMETRY_RESULT_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.DateTimeResponse.DATE_TIME_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.DrawGeometryResult.GEOMETRY_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.MULTIPLE_CHOICE_RESPONSES_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.MultipleChoiceResponses.OTHER_SELECTED_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.MultipleChoiceResponses.OTHER_TEXT_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.MultipleChoiceResponses.SELECTED_OPTION_IDS_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.NUMBER_RESPONSE_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.NumberResponse.NUMBER_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.SKIPPED_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.TASK_ID_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.TEXT_RESPONSE_FIELD_NUMBER
import com.google.android.ground.proto.TaskData.TextResponse.TEXT_FIELD_NUMBER
import com.google.common.truth.Truth.assertThat
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
  private val clientTimestamp = Date.from(Instant.ofEpochSecond(987654321))

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

  private val singleChoiceResponseOther =
    MultipleChoiceTaskData.fromList(
      MultipleChoice(
        persistentListOf(
          Option("option id 1", "code1", "Option 1"),
          Option("option id 2", "code2", "Option 2"),
        ),
        MultipleChoice.Cardinality.SELECT_ONE,
      ),
      ids = listOf("[ other value ]"),
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

  private val multipleChoiceTaskDataOther =
    MultipleChoiceTaskData.fromList(
      MultipleChoice(
        persistentListOf(
          Option("option id 1", "code1", "Option 1"),
          Option("option id 2", "code2", "Option 2"),
        ),
        MultipleChoice.Cardinality.SELECT_MULTIPLE,
      ),
      ids = listOf("option id 1", "option id 2", "[ other value ]"),
    )

  private val multipleChoiceTaskDataOtherEmpty =
    MultipleChoiceTaskData.fromList(
      MultipleChoice(
        persistentListOf(
          Option("option id 1", "code1", "Option 1"),
          Option("option id 2", "code2", "Option 2"),
        ),
        MultipleChoice.Cardinality.SELECT_MULTIPLE,
      ),
      ids = listOf("[  ]"),
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

  private val dateTaskResult = DateTimeTaskData(1725537603066)

  private val submissionMutation =
    SubmissionMutation(
      id = 1,
      submissionId = "submission_id",
      surveyId = "id_1",
      locationOfInterestId = loiId,
      userId = user.id,
      clientTimestamp = clientTimestamp,
      job = job,
      collectionId = "collectionId",
      deltas =
        listOf(
          ValueDelta(taskId = "text_task", taskType = Task.Type.TEXT, newTaskData = textTaskData),
          ValueDelta(
            taskId = "single_choice_task",
            taskType = Task.Type.MULTIPLE_CHOICE,
            newTaskData = singleChoiceResponse,
          ),
          ValueDelta(
            taskId = "single_choice_task_other",
            taskType = Task.Type.MULTIPLE_CHOICE,
            newTaskData = singleChoiceResponseOther,
          ),
          ValueDelta(
            taskId = "multiple_choice_task",
            taskType = Task.Type.MULTIPLE_CHOICE,
            newTaskData = multipleChoiceTaskData,
          ),
          ValueDelta(
            taskId = "multiple_choice_task_other",
            taskType = Task.Type.MULTIPLE_CHOICE,
            newTaskData = multipleChoiceTaskDataOther,
          ),
          ValueDelta(
            taskId = "multiple_choice_task_other_empty_value",
            taskType = Task.Type.MULTIPLE_CHOICE,
            newTaskData = multipleChoiceTaskDataOtherEmpty,
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
          ValueDelta(taskId = "time_task", taskType = Task.Type.TIME, newTaskData = dateTaskResult),
        ),
    )

  private val expected =
    listOf(
      mapOf(
        TEXT_RESPONSE_FIELD_NUMBER.toString() to mapOf(TEXT_FIELD_NUMBER.toString() to "some data"),
        TASK_ID_FIELD_NUMBER.toString() to "text_task",
      ),
      mapOf(
        MULTIPLE_CHOICE_RESPONSES_FIELD_NUMBER.toString() to
          mapOf(SELECTED_OPTION_IDS_FIELD_NUMBER.toString() to listOf("option id 1")),
        TASK_ID_FIELD_NUMBER.toString() to "single_choice_task",
      ),
      mapOf(
        MULTIPLE_CHOICE_RESPONSES_FIELD_NUMBER.toString() to
          mapOf(
            OTHER_TEXT_FIELD_NUMBER.toString() to "other value",
            OTHER_SELECTED_FIELD_NUMBER.toString() to true,
          ),
        TASK_ID_FIELD_NUMBER.toString() to "single_choice_task_other",
      ),
      mapOf(
        MULTIPLE_CHOICE_RESPONSES_FIELD_NUMBER.toString() to
          mapOf(
            SELECTED_OPTION_IDS_FIELD_NUMBER.toString() to listOf("option id 1", "option id 2")
          ),
        TASK_ID_FIELD_NUMBER.toString() to "multiple_choice_task",
      ),
      mapOf(
        MULTIPLE_CHOICE_RESPONSES_FIELD_NUMBER.toString() to
          mapOf(
            SELECTED_OPTION_IDS_FIELD_NUMBER.toString() to listOf("option id 1", "option id 2"),
            OTHER_TEXT_FIELD_NUMBER.toString() to "other value",
            OTHER_SELECTED_FIELD_NUMBER.toString() to true,
          ),
        TASK_ID_FIELD_NUMBER.toString() to "multiple_choice_task_other",
      ),
      mapOf(
        MULTIPLE_CHOICE_RESPONSES_FIELD_NUMBER.toString() to
          mapOf(OTHER_SELECTED_FIELD_NUMBER.toString() to true),
        TASK_ID_FIELD_NUMBER.toString() to "multiple_choice_task_other_empty_value",
      ),
      mapOf(
        NUMBER_RESPONSE_FIELD_NUMBER.toString() to mapOf(NUMBER_FIELD_NUMBER.toString() to 123.0),
        TASK_ID_FIELD_NUMBER.toString() to "number_task",
      ),
      mapOf(
        DRAW_GEOMETRY_RESULT_FIELD_NUMBER.toString() to
          mapOf(
            GEOMETRY_FIELD_NUMBER.toString() to
              mapOf(
                POINT_FIELD_NUMBER.toString() to
                  mapOf(
                    COORDINATES_FIELD_NUMBER.toString() to
                      mapOf(
                        LATITUDE_FIELD_NUMBER.toString() to 10.0,
                        LONGITUDE_FIELD_NUMBER.toString() to 20.0,
                      )
                  )
              )
          ),
        TASK_ID_FIELD_NUMBER.toString() to "drop_pin_task",
      ),
      mapOf(
        DRAW_GEOMETRY_RESULT_FIELD_NUMBER.toString() to
          mapOf(
            GEOMETRY_FIELD_NUMBER.toString() to
              mapOf(
                POLYGON_FIELD_NUMBER.toString() to
                  mapOf(
                    SHELL_FIELD_NUMBER.toString() to
                      mapOf(
                        COORDINATES_FIELD_NUMBER.toString() to
                          listOf(
                            mapOf(
                              LATITUDE_FIELD_NUMBER.toString() to 10.0,
                              LONGITUDE_FIELD_NUMBER.toString() to 20.0,
                            ),
                            mapOf(
                              LATITUDE_FIELD_NUMBER.toString() to 20.0,
                              LONGITUDE_FIELD_NUMBER.toString() to 30.0,
                            ),
                            mapOf(
                              LATITUDE_FIELD_NUMBER.toString() to 30.0,
                              LONGITUDE_FIELD_NUMBER.toString() to 20.0,
                            ),
                            mapOf(
                              LATITUDE_FIELD_NUMBER.toString() to 10.0,
                              LONGITUDE_FIELD_NUMBER.toString() to 20.0,
                            ),
                          )
                      )
                  )
              )
          ),
        TASK_ID_FIELD_NUMBER.toString() to "draw_area_task",
      ),
      mapOf(
        CAPTURE_LOCATION_RESULT_FIELD_NUMBER.toString() to
          mapOf(
            COORDINATES_FIELD_NUMBER.toString() to
              mapOf(
                LATITUDE_FIELD_NUMBER.toString() to 10.0,
                LONGITUDE_FIELD_NUMBER.toString() to 20.0,
              ),
            ACCURACY_FIELD_NUMBER.toString() to 80.8,
            ALTITUDE_FIELD_NUMBER.toString() to 112.31,
          ),
        TASK_ID_FIELD_NUMBER.toString() to "capture_location",
      ),
      mapOf(
        DATE_TIME_RESPONSE_FIELD_NUMBER.toString() to
          mapOf(DATE_TIME_FIELD_NUMBER.toString() to mapOf("1" to 1725537603L)),
        TASK_ID_FIELD_NUMBER.toString() to "date_task",
      ),
      mapOf(
        DATE_TIME_RESPONSE_FIELD_NUMBER.toString() to
          mapOf(DATE_TIME_FIELD_NUMBER.toString() to mapOf("1" to 1725537603L)),
        TASK_ID_FIELD_NUMBER.toString() to "time_task",
      ),
    )

  private val auditInfoObject =
    mapOf(
      USER_ID_FIELD_NUMBER.toString() to user.id,
      DISPLAY_NAME_FIELD_NUMBER.toString() to user.displayName,
      CLIENT_TIMESTAMP_FIELD_NUMBER.toString() to mapOf("1" to 987654321L),
      SERVER_TIMESTAMP_FIELD_NUMBER.toString() to mapOf("1" to 987654321L),
    )

  @Test
  fun testToMap_create() {
    val submissionMutation = submissionMutation.copy(type = Mutation.Type.CREATE)

    val map = submissionMutation.createSubmissionMessage(user).toFirestoreMap()

    assertThat(map[ID_FIELD_NUMBER.toString()]).isEqualTo(submissionMutation.submissionId)
    assertThat(map[LOI_ID_FIELD_NUMBER.toString()])
      .isEqualTo(submissionMutation.locationOfInterestId)
    assertThat(map[JOB_ID_FIELD_NUMBER.toString()]).isEqualTo(submissionMutation.job.id)
    assertThat(map[OWNER_ID_FIELD_NUMBER.toString()]).isEqualTo(submissionMutation.userId)
    assertThat(map[CREATED_FIELD_NUMBER.toString()]).isEqualTo(auditInfoObject)
    assertThat(map[LAST_MODIFIED_FIELD_NUMBER.toString()]).isEqualTo(auditInfoObject)
    assertThat(map[TASK_DATA_FIELD_NUMBER.toString()]).isEqualTo(expected)
  }

  @Test
  fun testToMap_update() {
    val submissionMutation = submissionMutation.copy(type = Mutation.Type.UPDATE)

    val map = submissionMutation.createSubmissionMessage(user).toFirestoreMap()

    assertThat(map[ID_FIELD_NUMBER.toString()]).isEqualTo(submissionMutation.submissionId)
    assertThat(map[LOI_ID_FIELD_NUMBER.toString()])
      .isEqualTo(submissionMutation.locationOfInterestId)
    assertThat(map[JOB_ID_FIELD_NUMBER.toString()]).isEqualTo(submissionMutation.job.id)
    assertThat(map[OWNER_ID_FIELD_NUMBER.toString()]).isEqualTo(submissionMutation.userId)
    assertThat(map[LAST_MODIFIED_FIELD_NUMBER.toString()]).isEqualTo(auditInfoObject)
    assertThat(map[TASK_DATA_FIELD_NUMBER.toString()]).isEqualTo(expected)
  }

  @Test
  fun testToMap_delete() {
    assertThrows("Unsupported mutation type", UnsupportedOperationException::class.java) {
      submissionMutation.copy(type = Mutation.Type.DELETE).createSubmissionMessage(user)
    }
  }

  @Test
  fun testToMap_unknown() {
    assertThrows("Unsupported mutation type", UnsupportedOperationException::class.java) {
      submissionMutation.copy(type = Mutation.Type.UNKNOWN).createSubmissionMessage(user)
    }
  }

  @Test
  fun testToMap_allTaskDataSkipped() {
    val mutation =
      submissionMutation.copy(
        job =
          job.copy(
            tasks =
              mapOf(
                Pair("task 1", Task("task 1", 1, Task.Type.TEXT, "task 1", true)),
                Pair("task 2", Task("task 2", 2, Task.Type.NUMBER, "task 2", true)),
              )
          ),
        deltas =
          listOf(
            ValueDelta(
              taskId = "task 1",
              taskType = Task.Type.TEXT,
              newTaskData = SkippedTaskData(),
            ),
            ValueDelta(
              taskId = "task 2",
              taskType = Task.Type.NUMBER,
              newTaskData = SkippedTaskData(),
            ),
          ),
        type = Mutation.Type.CREATE,
      )

    val map = mutation.createSubmissionMessage(user).toFirestoreMap()

    // Only verify "skipped" field as rest of the details have already been verified in previous one
    assertThat(map[TASK_DATA_FIELD_NUMBER.toString()])
      .isEqualTo(
        listOf(
          mapOf(
            TASK_ID_FIELD_NUMBER.toString() to "task 1",
            SKIPPED_FIELD_NUMBER.toString() to true,
          ),
          mapOf(
            TASK_ID_FIELD_NUMBER.toString() to "task 2",
            SKIPPED_FIELD_NUMBER.toString() to true,
          ),
        )
      )
  }

  @Test
  fun testToMap_someTaskDataSkipped() {
    val mutation =
      submissionMutation.copy(
        job =
          job.copy(
            tasks =
              mapOf(
                Pair("task 1", Task("task 1", 1, Task.Type.TEXT, "task 1", true)),
                Pair("task 2", Task("task 2", 2, Task.Type.NUMBER, "task 2", true)),
              )
          ),
        deltas =
          listOf(
            ValueDelta(
              taskId = "task 1",
              taskType = Task.Type.TEXT,
              newTaskData = TextTaskData.fromString("some data"),
            ),
            ValueDelta(
              taskId = "task 2",
              taskType = Task.Type.NUMBER,
              newTaskData = SkippedTaskData(),
            ),
          ),
        type = Mutation.Type.CREATE,
      )

    val map = mutation.createSubmissionMessage(user).toFirestoreMap()

    // Only verify "skipped" field as rest of the details have already been verified in previous one
    assertThat(map[TASK_DATA_FIELD_NUMBER.toString()])
      .isEqualTo(
        listOf(
          mapOf(
            TEXT_RESPONSE_FIELD_NUMBER.toString() to
              mapOf(TEXT_FIELD_NUMBER.toString() to "some data"),
            TASK_ID_FIELD_NUMBER.toString() to "task 1",
          ),
          mapOf(
            TASK_ID_FIELD_NUMBER.toString() to "task 2",
            SKIPPED_FIELD_NUMBER.toString() to true,
          ),
        )
      )
  }
}
