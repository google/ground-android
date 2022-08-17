/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.Survey
import com.google.android.ground.model.TestModelBuilders
import com.google.android.ground.model.User
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.Point
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.task.Task
import com.google.common.collect.ImmutableMap
import java8.util.Optional
import java.util.*

object DataCollectionTestData {
    private const val surveyId = "123"
    private const val loiId = "456"
    private const val submissionId = "789"
    const val jobName = "jobName"
    const val loiName = "loiName"
    val args = DataCollectionFragmentArgs.Builder(surveyId, loiId, submissionId).build()
    private val auditInfo = TestModelBuilders.newAuditInfo()
        .setUser(User("user1", "", ""))
        .setClientTimestamp(Date(100))
        .setServerTimestamp(Optional.of(Date(101)))
        .build()
    private val survey = Survey.newBuilder().setId(surveyId).setTitle("surveyTitle")
        .setDescription("surveyDescription").build()
    val submission: Submission = Submission.newBuilder()
        .setId(submissionId)
        .setSurvey(survey)
        .setLocationOfInterest(
            LocationOfInterest(
                loiId,
                survey,
                Job(name = jobName, id = "jobId"),
                null,
                loiName,
                auditInfo,
                auditInfo,
                Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build().toGeometry()
            )
        )
        .setCreated(auditInfo)
        .setLastModified(auditInfo)
        .setJob(
            Job(id = "taskId",
                tasks = ImmutableMap.of(
                    "field id",
                    Task("field id", 0, Task.Type.MULTIPLE_CHOICE, "field", true),
                    "field id 2",
                    Task("field id 2", 1, Task.Type.PHOTO, "field 2", true)))).build()
}