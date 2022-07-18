package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.Survey
import com.google.android.ground.model.TestModelBuilders
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.Point
import com.google.android.ground.model.locationofinterest.PointOfInterest
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.task.Field
import com.google.android.ground.model.task.Step
import com.google.android.ground.model.task.Task
import com.google.common.collect.ImmutableList
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
        .setUser(TestModelBuilders.newUser().setId("user1").build())
        .setClientTimestamp(Date(100))
        .setServerTimestamp(Optional.of(Date(101)))
        .build()
    private val survey = Survey.newBuilder().setId(surveyId).setTitle("surveyTitle")
        .setDescription("surveyDescription").build()
    val submission: Submission = Submission.newBuilder()
        .setId(submissionId)
        .setSurvey(survey)
        .setLocationOfInterest(
            PointOfInterest.newBuilder().setCaption(loiName).setSurvey(survey).setCreated(auditInfo)
                .setLastModified(auditInfo)
                .setPoint(Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build())
                .setJob(Job.newBuilder().setName(jobName).setId("jobId").build())
                .setId(loiId)
                .build()
        )
        .setCreated(auditInfo)
        .setLastModified(auditInfo)
        .setTask(
            Task.newBuilder().setId("taskId")
                .setSteps(
                    ImmutableList.of(
                        Step.ofField(
                            Field.newBuilder()
                                .setId("field id")
                                .setLabel("field")
                                .setIndex(0)
                                .setRequired(true)
                                .setType(Field.Type.MULTIPLE_CHOICE).build()
                        ),
                        Step.ofField(
                            Field.newBuilder()
                                .setId("field id 2")
                                .setLabel("field 2")
                                .setIndex(1)
                                .setRequired(true)
                                .setType(Field.Type.PHOTO).build()
                        )
                    )
                ).build()
        )
        .build()
}