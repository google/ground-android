/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.persistence.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.repository.SurveyRepository
import com.sharedtest.FakeData
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySyncServiceTest : BaseHiltTest() {
  @Inject
  @ApplicationContext
  lateinit var context: Context

  @Mock
  private lateinit var surveyRepository: SurveyRepository

  private lateinit var workManager: WorkManager
  private lateinit var testDriver: TestDriver

  @Before
  override fun setUp() {
    super.setUp()

    `when`(surveyRepository.syncSurveyWithRemote(Mockito.anyString()))
      .thenReturn(Single.just(FakeData.SURVEY))

    val config = Configuration.Builder()
      .setMinimumLoggingLevel(Log.VERBOSE)
      .setExecutor(SynchronousExecutor())
      .setWorkerFactory(
        object : WorkerFactory() {
          override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
          ) = SurveySyncWorker(context, workerParameters, surveyRepository)
        }
      )
      .build()

    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    workManager = WorkManager.getInstance(context)
    testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
  }

  @Test
  fun callSyncSurveyWithIdWhenConstraintsMet() {
    val surveyId = "survey1000"

    val service = SurveySyncService(workManager)
    val requestId = service.enqueueSync(surveyId)

    // Tell the testing framework that the constraints have been met and to run the worker.
    testDriver.setAllConstraintsMet(requestId)

    assertEquals(WorkInfo.State.SUCCEEDED, workManager.getWorkInfoById(requestId).get().state)
    verify(surveyRepository).syncSurveyWithRemote(surveyId)
  }
}
