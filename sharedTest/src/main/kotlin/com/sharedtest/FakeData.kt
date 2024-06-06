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
package com.sharedtest

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.TermsOfService
import com.google.android.ground.model.User
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.job.Style
import com.google.android.ground.model.locationofinterest.LOI_NAME_PROPERTY
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
import com.google.android.ground.ui.map.gms.features.FeatureClusterItem
import java.util.Date

/**
 * Shared test data constants. Tests are expected to override existing or set missing values when
 * the specific value is relevant to the test.
 */
object FakeData {
  // TODO: Replace constants with calls to newFoo() methods.
  val TERMS_OF_SERVICE: TermsOfService =
    TermsOfService("TERMS_OF_SERVICE", "Fake Terms of Service text")
  const val JOB_ID = "job id"
  const val LOI_ID = "loi id"
  const val USER_ID = "user id"
  const val SURVEY_ID = "survey id"

  val JOB =
    Job(
      name = "Job",
      id = "JOB",
      style = Style("#000"),
      strategy = Job.DataCollectionStrategy.MIXED,
    )

  val USER = User("user_id", "user@gmail.com", "User")

  val SURVEY: Survey =
    Survey(
      "SURVEY",
      "Survey title",
      "Test survey description",
      mapOf(JOB.id to JOB),
      listOf(),
      mapOf(USER.email to "data-collector"),
    )

  const val LOCATION_OF_INTEREST_NAME = "Test LOI Name"

  val LOCATION_OF_INTEREST =
    LocationOfInterest(
      LOI_ID,
      SURVEY.id,
      JOB,
      customId = "",
      created = AuditInfo(USER),
      lastModified = AuditInfo(USER),
      geometry = Point(Coordinates(0.0, 0.0)),
    )

  val LOCATION_OF_INTEREST_FEATURE =
    Feature(
      id = LOCATION_OF_INTEREST.id,
      type = FeatureType.LOCATION_OF_INTEREST.ordinal,
      geometry = LOCATION_OF_INTEREST.geometry,
      style = Feature.Style(0),
      clusterable = true,
    )

  val LOCATION_OF_INTEREST_CLUSTER_ITEM = FeatureClusterItem(LOCATION_OF_INTEREST_FEATURE)

  private val VERTICES: List<Point> =
    listOf(
      Point(Coordinates(0.0, 0.0)),
      Point(Coordinates(10.0, 10.0)),
      Point(Coordinates(20.0, 20.0)),
      Point(Coordinates(0.0, 0.0)),
    )

  private val AUDIT_INFO = AuditInfo(USER)

  val AREA_OF_INTEREST: LocationOfInterest =
    LocationOfInterest(
      LOI_ID,
      SURVEY.id,
      JOB,
      "",
      AUDIT_INFO,
      AUDIT_INFO,
      Polygon(LinearRing(VERTICES.map { it.coordinates })),
    )

  val COORDINATES = Coordinates(42.0, 18.0)

  private const val SUBMISSION_ID = "789"
  private const val TASK_1_NAME = "task 1"
  private const val TASK_2_NAME = "task 2"

  val SUBMISSION: Submission =
    Submission(
      SUBMISSION_ID,
      SURVEY.id,
      LOCATION_OF_INTEREST,
      JOB.copy(
        tasks =
          mapOf(
            "field id" to Task("field id", 0, Task.Type.TEXT, TASK_1_NAME, true),
            "field id 2" to Task("field id 2", 1, Task.Type.TEXT, TASK_2_NAME, true),
          )
      ),
      AUDIT_INFO,
      AUDIT_INFO,
    )

  fun newTask(
    id: String = "",
    type: Task.Type = Task.Type.TEXT,
    multipleChoice: MultipleChoice? = null,
  ): Task = Task(id, 0, type, "", false, multipleChoice)

  fun newLoiMutation(
    point: Point,
    mutationType: Mutation.Type = Mutation.Type.CREATE,
    syncStatus: Mutation.SyncStatus = Mutation.SyncStatus.PENDING,
  ): LocationOfInterestMutation =
    LocationOfInterestMutation(
      jobId = JOB_ID,
      geometry = point,
      id = 1L,
      locationOfInterestId = LOI_ID,
      type = mutationType,
      syncStatus = syncStatus,
      userId = USER_ID,
      surveyId = SURVEY_ID,
      clientTimestamp = Date(),
      properties = mapOf(LOI_NAME_PROPERTY to LOCATION_OF_INTEREST_NAME),
    )

  fun newAoiMutation(
    polygonVertices: List<Coordinates>,
    mutationType: Mutation.Type = Mutation.Type.CREATE,
    syncStatus: Mutation.SyncStatus = Mutation.SyncStatus.PENDING,
  ): LocationOfInterestMutation =
    LocationOfInterestMutation(
      jobId = JOB_ID,
      geometry = Polygon(LinearRing(polygonVertices)),
      id = 1L,
      locationOfInterestId = LOI_ID,
      type = mutationType,
      syncStatus = syncStatus,
      userId = USER_ID,
      surveyId = SURVEY_ID,
      clientTimestamp = Date(),
      properties = mapOf(LOI_NAME_PROPERTY to LOCATION_OF_INTEREST_NAME),
    )
}
