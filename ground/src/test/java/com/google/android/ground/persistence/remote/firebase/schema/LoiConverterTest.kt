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
package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.job.Style
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.remote.firebase.schema.LoiConverter.toLoi
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.sharedtest.FakeData.newTask
import java.util.*
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class LoiConverterTest {
  @Mock private lateinit var loiDocumentSnapshot: DocumentSnapshot

  private lateinit var survey: Survey
  private lateinit var noVerticesGeometry: MutableMap<String, Any>

  @Test
  fun testToLoi_whenNullLocation_returnsFailure() {
    setUpTestGeometry()
    setUpTestSurvey(
      "job001",
      newTask("task1"),
      newTask(
        "task2",
        Task.Type.MULTIPLE_CHOICE,
        MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
      ),
      newTask("task3", Task.Type.MULTIPLE_CHOICE),
      newTask("task4", Task.Type.PHOTO),
    )
    mockLoiDocumentSnapshot(
      "loi001",
      LoiDocument(
        /* jobId */ "job001",
        /* customId */ null,
        /* location */ null,
        /* geoJson */ null,
        /* geometry */ null,
        /* created */ AUDIT_INFO_1_NESTED_OBJECT,
        /* lastModified */ AUDIT_INFO_2_NESTED_OBJECT,
      ),
    )
    assertThat(toLocationOfInterest().isFailure).isTrue()
  }

  @Test
  fun testToLoi_whenZeroVertices_returnsFailure() {
    setUpTestGeometry()
    setUpTestSurvey(
      "job001",
      newTask("task1"),
      newTask(
        "task2",
        Task.Type.MULTIPLE_CHOICE,
        MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
      ),
      newTask("task3", Task.Type.MULTIPLE_CHOICE),
      newTask("task4", Task.Type.PHOTO),
    )
    mockLoiDocumentSnapshot(
      "loi001",
      LoiDocument(
        "job001",
        null,
        null,
        null,
        noVerticesGeometry,
        AUDIT_INFO_1_NESTED_OBJECT,
        AUDIT_INFO_2_NESTED_OBJECT,
      ),
    )
    assertThat(toLocationOfInterest().isFailure).isTrue()
  }

  private fun setUpTestSurvey(jobId: String, vararg tasks: Task) {
    val taskMap = tasks.associateBy { it.id }
    val job = Job(jobId, TEST_STYLE, "JOB_NAME", taskMap)
    survey = Survey("", "", "", mapOf(Pair(job.id, job)))
  }

  private fun setUpTestGeometry() {
    noVerticesGeometry = HashMap()
    noVerticesGeometry[LoiConverter.GEOMETRY_TYPE] = LoiConverter.POLYGON_TYPE
  }

  /** Mock submission document snapshot to return the specified id and object representation. */
  private fun mockLoiDocumentSnapshot(id: String, doc: LoiDocument) {
    whenever(loiDocumentSnapshot.id).thenReturn(id)
    whenever(loiDocumentSnapshot.toObject(LoiDocument::class.java)).thenReturn(doc)
    whenever(loiDocumentSnapshot.exists()).thenReturn(true)
  }

  private fun toLocationOfInterest(): Result<LocationOfInterest> =
    toLoi(survey, loiDocumentSnapshot)

  companion object {
    private val TEST_STYLE = Style("#112233")
    private val AUDIT_INFO_1_NESTED_OBJECT =
      AuditInfoNestedObject(
        UserNestedObject("user1", null, null),
        Timestamp(Date(100)),
        Timestamp(Date(101)),
      )
    private val AUDIT_INFO_2_NESTED_OBJECT =
      AuditInfoNestedObject(
        UserNestedObject("user2", null, null),
        Timestamp(Date(200)),
        Timestamp(Date(201)),
      )
  }
}
