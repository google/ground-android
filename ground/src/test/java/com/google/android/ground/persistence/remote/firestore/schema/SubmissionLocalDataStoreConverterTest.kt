/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.persistence.remote.firestore.schema

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.User
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskDataMap
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firestore.schema.SubmissionConverter.toSubmission
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.sharedtest.FakeData
import com.sharedtest.FakeData.newTask
import java.util.*
import java8.util.Optional
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SubmissionLocalDataStoreConverterTest {
  @Mock private lateinit var submissionDocumentSnapshot: DocumentSnapshot

  private lateinit var job: Job
  private lateinit var locationOfInterest: LocationOfInterest

  @Test
  fun testToSubmission() {
    setUpTestSurvey(
      "job001",
      "loi001",
      newTask("task1"),
      newTask(
        "task2",
        Task.Type.MULTIPLE_CHOICE,
        MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE)
      ),
      newTask("task3", Task.Type.MULTIPLE_CHOICE),
      newTask("task4", Task.Type.PHOTO)
    )
    mockSubmissionDocumentSnapshot(
      SUBMISSION_ID,
      SubmissionDocument(
        "loi001",
        "task001",
        AUDIT_INFO_1_NESTED_OBJECT,
        AUDIT_INFO_2_NESTED_OBJECT,
        ImmutableMap.of(
          "task1",
          "Text taskData",
          "task2",
          ImmutableList.of("option2"),
          "task3",
          ImmutableList.of("optionA", "optionB"),
          "task4",
          "Photo URL"
        )
      )
    )
    assertThat(toSubmission())
      .isEqualTo(
        Submission(
          SUBMISSION_ID,
          TEST_SURVEY_ID,
          locationOfInterest,
          job,
          AUDIT_INFO_1,
          AUDIT_INFO_2,
          TaskDataMap(
            ImmutableMap.of(
              "task1",
              TextTaskData("Text taskData"),
              "task2",
              MultipleChoiceTaskData(
                MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
                ImmutableList.of("option2")
              ),
              "task3",
              MultipleChoiceTaskData(
                MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
                ImmutableList.of("optionA", "optionB")
              ),
              "task4",
              TextTaskData("Photo URL")
            )
          )
        )
      )
  }

  @Test
  fun testToSubmission_mismatchedLoiId() {
    setUpTestSurvey("job001", "loi001", newTask("task1"))
    mockSubmissionDocumentSnapshot(
      SUBMISSION_ID,
      SubmissionDocument(
        "loi999",
        "task001",
        AUDIT_INFO_1_NESTED_OBJECT,
        AUDIT_INFO_2_NESTED_OBJECT,
        ImmutableMap.of("task1", "")
      )
    )
    Assert.assertThrows(DataStoreException::class.java) { this.toSubmission() }
  }

  @Test
  fun testToSubmission_nullResponses() {
    setUpTestSurvey("job001", "loi001", newTask("task1"))
    mockSubmissionDocumentSnapshot(
      SUBMISSION_ID,
      SubmissionDocument(
        "loi001",
        "task001",
        AUDIT_INFO_1_NESTED_OBJECT,
        AUDIT_INFO_2_NESTED_OBJECT,
        null
      )
    )
    assertThat(toSubmission())
      .isEqualTo(
        Submission(
          SUBMISSION_ID,
          TEST_SURVEY_ID,
          locationOfInterest,
          job,
          AUDIT_INFO_1,
          AUDIT_INFO_2
        )
      )
  }

  @Test
  fun testToSubmission_emptyTextResponse() {
    setUpTestSurvey("job001", "loi001", newTask("task1"))
    mockSubmissionDocumentSnapshot(
      SUBMISSION_ID,
      SubmissionDocument(
        "loi001",
        "task001",
        AUDIT_INFO_1_NESTED_OBJECT,
        AUDIT_INFO_2_NESTED_OBJECT,
        ImmutableMap.of("task1", "")
      )
    )
    assertThat(toSubmission())
      .isEqualTo(
        Submission(
          SUBMISSION_ID,
          TEST_SURVEY_ID,
          locationOfInterest,
          job,
          AUDIT_INFO_1,
          AUDIT_INFO_2
        )
      )
  }

  @Test
  fun testToSubmission_emptyMultipleChoiceResponse() {
    setUpTestSurvey("job001", "loi001", newTask("task1"))
    mockSubmissionDocumentSnapshot(
      SUBMISSION_ID,
      SubmissionDocument(
        "loi001",
        "task001",
        AUDIT_INFO_1_NESTED_OBJECT,
        AUDIT_INFO_2_NESTED_OBJECT,
        ImmutableMap.of("task1", ImmutableList.of<Any>())
      )
    )
    assertThat(toSubmission())
      .isEqualTo(
        Submission(
          SUBMISSION_ID,
          TEST_SURVEY_ID,
          locationOfInterest,
          job,
          AUDIT_INFO_1,
          AUDIT_INFO_2
        )
      )
  }

  @Test
  fun testToSubmission_unknownFieldType() {
    setUpTestSurvey("job001", "loi001", newTask("task1", Task.Type.UNKNOWN), newTask("task2"))
    mockSubmissionDocumentSnapshot(
      SUBMISSION_ID,
      SubmissionDocument(
        "loi001",
        "task001",
        AUDIT_INFO_1_NESTED_OBJECT,
        AUDIT_INFO_2_NESTED_OBJECT,
        ImmutableMap.of("task1", "Unknown", "task2", "Text taskData")
      )
    )
    assertThat(toSubmission())
      .isEqualTo(
        Submission(
          SUBMISSION_ID,
          TEST_SURVEY_ID,
          locationOfInterest,
          job,
          AUDIT_INFO_1,
          AUDIT_INFO_2,
          // Field "task1" with unknown field type ignored.
          TaskDataMap(ImmutableMap.of("task2", TextTaskData("Text taskData")))
        )
      )
  }

  private fun setUpTestSurvey(jobId: String, loiId: String, vararg tasks: Task) {
    val taskMap = ImmutableMap.builder<String, Task>()
    tasks.forEach { task: Task -> taskMap.put(task.id, task) }
    job = Job(jobId, "JOB_NAME", taskMap.build())
    locationOfInterest =
      FakeData.LOCATION_OF_INTEREST.copy(id = loiId, surveyId = TEST_SURVEY_ID, job = job)
  }

  /** Mock submission document snapshot to return the specified id and object representation. */
  private fun mockSubmissionDocumentSnapshot(id: String, doc: SubmissionDocument) {
    Mockito.`when`(submissionDocumentSnapshot.id).thenReturn(id)
    Mockito.`when`(submissionDocumentSnapshot.toObject(SubmissionDocument::class.java))
      .thenReturn(doc)
  }

  private fun toSubmission(): Submission {
    return toSubmission(locationOfInterest, submissionDocumentSnapshot)
  }

  companion object {
    private val AUDIT_INFO_1 = AuditInfo(User("user1", "", ""), Date(100), Optional.of(Date(101)))
    private val AUDIT_INFO_2 = AuditInfo(User("user2", "", ""), Date(200), Optional.of(Date(201)))
    private val AUDIT_INFO_1_NESTED_OBJECT =
      AuditInfoNestedObject(
        UserNestedObject("user1", null, null),
        Timestamp(Date(100)),
        Timestamp(Date(101))
      )
    private val AUDIT_INFO_2_NESTED_OBJECT =
      AuditInfoNestedObject(
        UserNestedObject("user2", null, null),
        Timestamp(Date(200)),
        Timestamp(Date(201))
      )
    private const val SUBMISSION_ID = "submission123"
    private const val TEST_SURVEY_ID = "survey001"
  }
}
