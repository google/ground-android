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
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.FeatureClusterItem
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap

/**
 * Shared test data constants. Tests are expected to override existing or set missing values when
 * the specific value is relevant to the test.
 */
object FakeData {
  // TODO: Replace constants with calls to newFoo() methods.
  @JvmField
  val TERMS_OF_SERVICE: TermsOfService =
    TermsOfService("TERMS_OF_SERVICE", "Fake Terms of Service text")

  @JvmField val JOB = Job(name = "Job", id = "JOB")

  @JvmField val USER = User("user_id", "user@gmail.com", "User")

  @JvmField val USER_2 = User("user_id_2", "user2@gmail.com", "User2")

  @JvmField
  val SURVEY: Survey =
    Survey(
      "SURVEY",
      "Survey title",
      "Test survey description",
      ImmutableMap.of(),
      ImmutableList.of(),
      ImmutableMap.of(USER.email, "data-collector")
    )

  @JvmField
  val LOCATION_OF_INTEREST =
    LocationOfInterest(
      "loi id",
      SURVEY.id,
      JOB,
      null,
      "loi name",
      AuditInfo(USER),
      AuditInfo(USER),
      Point(Coordinate(0.0, 0.0))
    )

  @JvmField
  val LOCATION_OF_INTEREST_FEATURE =
    Feature(
      LOCATION_OF_INTEREST.id,
      Feature.Type.LOCATION_OF_INTEREST,
      LOCATION_OF_INTEREST.geometry
    )

  @JvmField val LOCATION_OF_INTEREST_CLUSTER_ITEM = FeatureClusterItem(LOCATION_OF_INTEREST_FEATURE)

  @JvmField
  val VERTICES: ImmutableList<Point> =
    ImmutableList.of(
      Point(Coordinate(0.0, 0.0)),
      Point(Coordinate(10.0, 10.0)),
      Point(Coordinate(20.0, 20.0)),
      Point(Coordinate(0.0, 0.0)),
    )

  private val AUDIT_INFO = AuditInfo(USER)

  @JvmField
  val AREA_OF_INTEREST: LocationOfInterest =
    LocationOfInterest(
      "loi id",
      SURVEY.id,
      JOB,
      "",
      "",
      AUDIT_INFO,
      AUDIT_INFO,
      Polygon(LinearRing(VERTICES.map { it.coordinate })),
    )

  @JvmField val POINT = Point(Coordinate(42.0, 18.0))

  @JvmStatic
  @JvmOverloads
  fun newTask(
    id: String = "",
    type: Task.Type = Task.Type.TEXT,
    multipleChoice: MultipleChoice? = null
  ): Task = Task(id, 0, type, "", false, multipleChoice)

  private const val SUBMISSION_ID = "789"
  const val TASK_1_NAME = "task 1"
  const val TASK_2_NAME = "task 2"

  val SUBMISSION: Submission =
    Submission(
      SUBMISSION_ID,
      SURVEY.id,
      LOCATION_OF_INTEREST,
      JOB.copy(
        id = "taskId",
        tasks =
          ImmutableMap.of(
            "field id",
            Task("field id", 0, Task.Type.TEXT, TASK_1_NAME, true),
            "field id 2",
            Task("field id 2", 1, Task.Type.TEXT, TASK_2_NAME, true)
          )
      ),
      AUDIT_INFO,
      AUDIT_INFO
    )
}
