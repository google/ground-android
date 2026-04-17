/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.testing

import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.job.Style
import org.groundplatform.domain.model.locationofinterest.AuditInfo
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.model.settings.UserSettings
import org.groundplatform.domain.model.submission.DraftSubmission
import org.groundplatform.domain.model.submission.ValueDelta
import org.groundplatform.domain.model.task.Condition
import org.groundplatform.domain.model.task.MultipleChoice
import org.groundplatform.domain.model.task.Task

object FakeDataGenerator {
  fun newUser(): User = User("user id", "", "User")

  fun newUserSettings(
    language: String = "en",
    measurementUnits: MeasurementUnits = MeasurementUnits.METRIC,
    shouldUploadPhotosOnWifiOnly: Boolean = false,
  ): UserSettings =
    UserSettings(
      language = language,
      measurementUnits = measurementUnits,
      shouldUploadPhotosOnWifiOnly = shouldUploadPhotosOnWifiOnly,
    )

  fun newJob(
    id: String = "job id",
    style: Style = Style("#000"),
    name: String = "job",
    tasks: Map<String, Task> = emptyMap(),
    strategy: Job.DataCollectionStrategy = Job.DataCollectionStrategy.PREDEFINED,
  ): Job = Job(id = id, style = style, name = name, tasks = tasks, strategy = strategy)

  @Suppress("StringLiteralDuplication")
  fun newLocationOfInterest(
    id: String = "loi id",
    surveyId: String = "survey id",
    job: Job = newJob(),
    customId: String = "",
    created: AuditInfo = AuditInfo(newUser()),
    lastModified: AuditInfo = AuditInfo(newUser()),
    geometry: Geometry = Point(Coordinates(0.0, 0.0)),
  ): LocationOfInterest =
    LocationOfInterest(
      id = id,
      surveyId = surveyId,
      job = job,
      customId = customId,
      created = created,
      lastModified = lastModified,
      geometry = geometry,
    )

  fun newDataSharingTerms(
    type: Survey.DataSharingTerms =
      Survey.DataSharingTerms.Custom(
        "## Introduction\n\nOnly one rule: **BE EXCELLENT TO ONE ANOTHER!**"
      )
  ): Survey.DataSharingTerms = type

  fun newGeneralAccess(
    type: Survey.GeneralAccess = Survey.GeneralAccess.RESTRICTED
  ): Survey.GeneralAccess = type

  fun newSurvey(
    id: String = "survey id",
    title: String = "Survey title",
    description: String = "Test survey description",
    jobMap: Map<String, Job> = mapOf(with(newJob()) { id to this }),
    acl: Map<String, String> = mapOf(with(newUser()) { email to "DATA_COLLECTOR" }),
    dataSharingTerms: Survey.DataSharingTerms = newDataSharingTerms(),
    generalAccess: Survey.GeneralAccess = newGeneralAccess(),
  ): Survey =
    Survey(
      id = id,
      title = title,
      description = description,
      jobMap = jobMap,
      acl = acl,
      dataSharingTerms = dataSharingTerms,
      generalAccess = generalAccess,
    )

  fun newDraftSubmission(
    id: String = "draft submission id",
    jobId: String = "job id",
    loiId: String? = "loi id",
    loiName: String? = null,
    surveyId: String = "survey id",
    deltas: List<ValueDelta> = emptyList(),
    currentTaskId: String = "task id",
  ): DraftSubmission =
    DraftSubmission(
      id = id,
      jobId = jobId,
      loiId = loiId,
      loiName = loiName,
      surveyId = surveyId,
      deltas = deltas,
      currentTaskId = currentTaskId,
    )

  fun newTask(
    id: String = "taskId",
    type: Task.Type = Task.Type.TEXT,
    multipleChoice: MultipleChoice? = null,
    isAddLoiTask: Boolean = false,
    condition: Condition? = null,
    isRequired: Boolean = false,
  ): Task =
    Task(
      id = id,
      index = 0,
      type = type,
      label = "",
      isRequired = isRequired,
      multipleChoice = multipleChoice,
      isAddLoiTask = isAddLoiTask,
      condition = condition,
    )
}
