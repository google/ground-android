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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.getOrAwaitValue
import com.google.android.ground.model.Survey
import com.google.android.ground.model.TestModelBuilders
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.Point
import com.google.android.ground.model.locationofinterest.PointOfInterest
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalDataStoreModule
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.reactivex.Single
import java8.util.Optional
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.*
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(LocalDataStoreModule::class)
@RunWith(RobolectricTestRunner::class)
class DataCollectionViewModelTest : BaseHiltTest() {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val surveyId = "123"
    private val loiId = "456"
    private val submissionId = "789"
    private val jobName = "jobName"
    private val loiName = "loiName"
    private val args = DataCollectionFragmentArgs.Builder(surveyId, loiId, submissionId).build()
    private val auditInfo = TestModelBuilders.newAuditInfo()
        .setUser(TestModelBuilders.newUser().setId("user1").build())
        .setClientTimestamp(Date(100))
        .setServerTimestamp(Optional.of(Date(101)))
        .build()
    private val survey = Survey.newBuilder().setId(surveyId).setTitle("surveyTitle")
        .setDescription("surveyDescription").build()
    private val submission = Submission.newBuilder()
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
        .setTask(Task.newBuilder().setId("taskId").build())
        .build()

    @BindValue
    @Mock
    lateinit var localDataStore: LocalDataStore

    @BindValue
    @Mock
    lateinit var submissionRepository: SubmissionRepository

    lateinit var dataCollectionViewModel: DataCollectionViewModel

    @Inject
    lateinit var locationOfInterestHelper: LocationOfInterestHelper

    @Before
    override fun setUp() {
        super.setUp()

        whenever(
            submissionRepository.createSubmission(
                any(),
                any(),
                any()
            )
        ).doReturn(Single.just(
            submission
        ))
        dataCollectionViewModel =
            DataCollectionViewModel(submissionRepository, locationOfInterestHelper)
    }

    @Test
    fun testJobNameIsSetCorrectly() {
        dataCollectionViewModel.loadSubmissionDetails(args)

        assertThat(dataCollectionViewModel.jobName.getOrAwaitValue())
            .isEqualTo(jobName)
    }

    @Test
    fun testLoiNameIsSetCorrectly() {
        dataCollectionViewModel.loadSubmissionDetails(args)

        assertThat(dataCollectionViewModel.loiName.getOrAwaitValue())
            .isEqualTo(loiName)
    }
}