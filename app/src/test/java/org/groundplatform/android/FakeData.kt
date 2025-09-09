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
package org.groundplatform.android

import java.util.Date
import org.groundplatform.android.model.AuditInfo
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.TermsOfService
import org.groundplatform.android.model.User
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.MultiPolygon
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.imagery.OfflineArea
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.locationofinterest.LOI_NAME_PROPERTY
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.model.task.Condition
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.proto.Survey.DataSharingTerms
import org.groundplatform.android.proto.SurveyKt.dataSharingTerms
import org.groundplatform.android.proto.copy
import org.groundplatform.android.ui.map.Bounds
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.FeatureType
import org.groundplatform.android.ui.map.gms.features.FeatureClusterItem

/**
 * Shared test data constants. Tests are expected to override existing or set missing values when
 * the specific value is relevant to the test.
 */
object FakeData {
  // TODO: Replace constants with calls to newFoo() methods.
  // Issue URL: https://github.com/google/ground-android/issues/2917
  val TERMS_OF_SERVICE: TermsOfService =
    TermsOfService("TERMS_OF_SERVICE", "Fake Terms of Service text")
  const val JOB_ID = "job id"
  const val LOI_ID = "loi id"
  const val USER_ID = "user id"
  const val SURVEY_ID = "survey id"
  const val SUBMISSION_ID = "submission id"
  const val TASK_ID = "task_id"

  val JOB =
    Job(
      name = "Job",
      id = JOB_ID,
      style = Style("#000"),
      strategy = Job.DataCollectionStrategy.PREDEFINED,
    )

  private val ADHOC_TASK =
    newTask("adhoc_task", Task.Type.CAPTURE_LOCATION).copy(isAddLoiTask = true)

  val ADHOC_JOB =
    Job(
      name = "Adhoc Job",
      id = "ADHOC_JOB",
      style = Style("#000"),
      strategy = Job.DataCollectionStrategy.AD_HOC,
      tasks = mapOf(ADHOC_TASK.id to ADHOC_TASK),
    )

  val USER = User(USER_ID, "", "User")

  val DATA_SHARING_TERMS = dataSharingTerms {
    type = DataSharingTerms.Type.CUSTOM
    customText = "## Introduction\n\nOnly one rule: **BE EXCELLENT TO ONE ANOTHER!**"
  }

  val FAKE_GENERAL_ACCESS = org.groundplatform.android.proto.Survey.GeneralAccess.RESTRICTED

  val SURVEY: Survey =
    Survey(
      SURVEY_ID,
      "Survey title",
      "Test survey description",
      mapOf(JOB.id to JOB, ADHOC_JOB.id to ADHOC_JOB),
      mapOf(USER.email to "DATA_COLLECTOR"),
      DATA_SHARING_TERMS.copy {},
      FAKE_GENERAL_ACCESS,
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

  val LOCATION_OF_INTEREST_WITH_MULTIPOLYGON =
    LOCATION_OF_INTEREST.copy(
      geometry = MultiPolygon(listOf(Polygon(LinearRing(VERTICES.map { it.coordinates }))))
    )

  val LOCATION_OF_INTEREST_WITH_LINEARRING =
    LOCATION_OF_INTEREST.copy(geometry = LinearRing(VERTICES.map { it.coordinates }))

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

  val OFFLINE_AREA =
    OfflineArea("id_1", OfflineArea.State.PENDING, Bounds(0.0, 0.0, 0.0, 0.0), "Test Area", 0..14)

  fun newTask(
    id: String = "taskId",
    type: Task.Type = Task.Type.TEXT,
    multipleChoice: MultipleChoice? = null,
    isAddLoiTask: Boolean = false,
    condition: Condition? = null,
  ): Task =
    Task(
      id = id,
      index = 0,
      type = type,
      label = "",
      isRequired = false,
      multipleChoice = multipleChoice,
      isAddLoiTask = isAddLoiTask,
      condition = condition,
    )

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
      collectionId = "",
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
      collectionId = "",
    )

  fun newSubmissionMutation(): SubmissionMutation =
    SubmissionMutation(
      type = Mutation.Type.CREATE,
      syncStatus = Mutation.SyncStatus.PENDING,
      surveyId = SURVEY_ID,
      locationOfInterestId = LOI_ID,
      userId = USER_ID,
      collectionId = "",
      job = JOB,
      submissionId = SUBMISSION_ID,
    )
}
