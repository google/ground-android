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
package org.groundplatform.android.persistence.remote.firebase.schema

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import com.google.protobuf.timestamp
import java.util.Date
import kotlinx.collections.immutable.persistentListOf
import org.groundplatform.android.FakeData.FAKE_GENERAL_ACCESS
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.FakeData.USER_ID
import org.groundplatform.android.FakeData.newTask
import org.groundplatform.android.assertIsSuccessWith
import org.groundplatform.android.model.AuditInfo
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.persistence.remote.firebase.protobuf.toFirestoreMap
import org.groundplatform.android.persistence.remote.firebase.schema.LoiConverter.toLoi
import org.groundplatform.android.proto.LocationOfInterest as LocationOfInterestProto
import org.groundplatform.android.proto.LocationOfInterest.Source
import org.groundplatform.android.proto.LocationOfInterestKt.property
import org.groundplatform.android.proto.auditInfo
import org.groundplatform.android.proto.coordinates
import org.groundplatform.android.proto.geometry
import org.groundplatform.android.proto.locationOfInterest
import org.groundplatform.android.proto.point
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

  private var testLoiProto = locationOfInterest {
    id = LOI_ID
    jobId = JOB_ID
    geometry = geometry {
      point = point {
        coordinates = coordinates {
          latitude = 1.0
          longitude = 2.0
        }
      }
    }
    submissionCount = 1
    ownerId = USER_ID
    created = auditInfo {
      userId = USER.id
      displayName = USER.displayName
      photoUrl = USER.photoUrl.orEmpty()
      clientTimestamp = timestamp { seconds = 987654321 }
      serverTimestamp = timestamp { seconds = 9876543210 }
    }
    lastModified = auditInfo {
      userId = USER.id
      displayName = USER.displayName
      photoUrl = USER.photoUrl.orEmpty()
      clientTimestamp = timestamp { seconds = 987654321 }
      serverTimestamp = timestamp { seconds = 9876543210 }
    }
    customTag = "a custom loi"
    source = Source.IMPORTED
    properties.put("property1", property { stringValue = "value1" })
    properties.put("property2", property { numericValue = 123.0 })
  }

  @Test
  fun `parses LocationOfInterest proto from DocumentSnapshot`() {
    setUpTestSurvey(
      JOB_ID,
      newTask("task1"),
      newTask(
        "task2",
        Task.Type.MULTIPLE_CHOICE,
        MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
      ),
      newTask("task3", Task.Type.MULTIPLE_CHOICE),
      newTask("task4", Task.Type.PHOTO),
    )
    mockLoiProtoDocumentSnapshot(LOI_ID, testLoiProto)
    assertIsSuccessWith(
      LocationOfInterest(
        id = LOI_ID,
        surveyId = "",
        job = survey.getJob(JOB_ID)!!,
        customId = "a custom loi",
        created = AuditInfo(user = USER, Date(987654321L * 1000), Date(9876543210L * 1000)),
        lastModified = AuditInfo(user = USER, Date(987654321L * 1000), Date(9876543210L * 1000)),
        geometry = Point(coordinates = Coordinates(1.0, 2.0)),
        submissionCount = 1,
        properties = mapOf("property1" to "value1", "property2" to 123.0),
        isPredefined = true,
      ),
      toLocationOfInterest(),
    )
  }

  @Test
  fun `fails when converting null location of interest`() {
    setUpTestGeometry()
    setUpTestSurvey(
      JOB_ID,
      newTask("task1"),
      newTask(
        "task2",
        Task.Type.MULTIPLE_CHOICE,
        MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
      ),
      newTask("task3", Task.Type.MULTIPLE_CHOICE),
      newTask("task4", Task.Type.PHOTO),
    )
    assertThat(toLocationOfInterest().isFailure).isTrue()
  }

  @Test
  fun `fails when converting location of interest with zero indices`() {
    setUpTestGeometry()
    setUpTestSurvey(
      JOB_ID,
      newTask("task1"),
      newTask(
        "task2",
        Task.Type.MULTIPLE_CHOICE,
        MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
      ),
      newTask("task3", Task.Type.MULTIPLE_CHOICE),
      newTask("task4", Task.Type.PHOTO),
    )
    assertThat(toLocationOfInterest().isFailure).isTrue()
  }

  private fun setUpTestSurvey(jobId: String, vararg tasks: Task) {
    val taskMap = tasks.associateBy { it.id }
    val job = Job(jobId, TEST_STYLE, "JOB_NAME", taskMap)
    survey = Survey("", "", "", mapOf(Pair(job.id, job)), generalAccess = FAKE_GENERAL_ACCESS)
  }

  private fun setUpTestGeometry() {
    noVerticesGeometry = HashMap()
    noVerticesGeometry[LoiConverter.GEOMETRY_TYPE] = LoiConverter.POLYGON_TYPE
  }

  /** Mock submission document snapshot to return the specified id and proto representation. */
  private fun mockLoiProtoDocumentSnapshot(id: String, loiProto: LocationOfInterestProto) {
    whenever(loiDocumentSnapshot.id).thenReturn(id)
    whenever(loiDocumentSnapshot.data).thenReturn(loiProto.toFirestoreMap())
    whenever(loiDocumentSnapshot.exists()).thenReturn(true)
  }

  private fun toLocationOfInterest(): Result<LocationOfInterest> =
    toLoi(survey, loiDocumentSnapshot)

  companion object {
    private const val JOB_ID = "job001"
    private const val LOI_ID = "loi001"
    private val TEST_STYLE = Style("#112233")
  }
}
