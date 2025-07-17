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

package org.groundplatform.android.persistence.sync

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.usecases.survey.SyncSurveyUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySyncServiceTest : BaseHiltTest() {
  @Inject @ApplicationContext lateinit var context: Context
  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
  @BindValue @Mock lateinit var syncSurvey: SyncSurveyUseCase

  private lateinit var workManager: WorkManager
  private lateinit var testDriver: TestDriver

  @Before
  override fun setUp() {
    super.setUp()

    val config =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.VERBOSE)
        .setExecutor(SynchronousExecutor())
        .setWorkerFactory(
          object : WorkerFactory() {
            override fun createWorker(
              appContext: Context,
              workerClassName: String,
              workerParameters: WorkerParameters,
            ) = SurveySyncWorker(context, workerParameters, syncSurvey, ioDispatcher)
          }
        )
        .build()
    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    workManager = WorkManager.getInstance(context)
    testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
  }

  @Test
  fun callSyncSurveyWithIdWhenConstraintsMet() = runWithTestDispatcher {
    `when`(syncSurvey(SURVEY.id)).thenReturn(SURVEY)

    val surveyId = "survey1000"

    val service = SurveySyncService(workManager)
    val requestId = service.enqueueSync(surveyId)

    // Tell the testing framework that the constraints have been met and to run the worker.
    testDriver.setAllConstraintsMet(requestId)
    advanceUntilIdle()

    verify(syncSurvey).invoke(surveyId)
    assertEquals(WorkInfo.State.SUCCEEDED, workManager.getWorkInfoById(requestId).await().state)
  }
}
