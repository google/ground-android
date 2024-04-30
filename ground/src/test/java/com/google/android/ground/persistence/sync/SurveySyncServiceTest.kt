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
import com.google.android.ground.domain.usecases.survey.SyncSurveyUseCase
import com.sharedtest.FakeData.SURVEY
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySyncServiceTest : BaseHiltTest() {
  @Inject @ApplicationContext lateinit var context: Context
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
            ) = SurveySyncWorker(context, workerParameters, syncSurvey)
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

    // TODO(#1787): Re-enable once GCB-specific flake is resolved.
    //    verify(syncSurvey).invoke(surveyId)
    //    assertEquals(WorkInfo.State.SUCCEEDED,
    // workManager.getWorkInfoById(requestId).await().state)
  }
}
