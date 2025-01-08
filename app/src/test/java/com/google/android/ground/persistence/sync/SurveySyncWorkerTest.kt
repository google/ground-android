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
import android.content.res.Resources.NotFoundException
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.domain.usecases.survey.SyncSurveyUseCase
import com.google.android.ground.persistence.sync.SurveySyncWorker.Companion.SURVEY_ID_PARAM_KEY
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.SURVEY
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySyncWorkerTest : BaseHiltTest() {
  private lateinit var context: Context
  @BindValue @Mock lateinit var syncSurvey: SyncSurveyUseCase

  private val factory =
    object : WorkerFactory() {
      override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
      ) = SurveySyncWorker(appContext, workerParameters, syncSurvey)
    }

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun `doWork() fails on no input`() = runWithTestDispatcher {
    val worker =
      TestListenableWorkerBuilder<SurveySyncWorker>(
          context,
          inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, null)),
        )
        .setWorkerFactory(factory)
        .build()
    val result = worker.doWork()
    assertThat(result).isEqualTo(Result.failure())
  }

  @Test
  fun `doWork() succeeds on valid survey`() = runWithTestDispatcher {
    `when`(syncSurvey(SURVEY.id)).thenReturn(SURVEY)

    val worker =
      TestListenableWorkerBuilder<SurveySyncWorker>(
          context,
          inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
        )
        .setWorkerFactory(factory)
        .build()
    val result = worker.doWork()
    assertThat(result).isEqualTo(Result.success())
  }

  @Test
  fun `doWork() retries on failure`() = runWithTestDispatcher {
    `when`(syncSurvey(SURVEY.id)).thenThrow(NotFoundException())

    val worker =
      TestListenableWorkerBuilder<SurveySyncWorker>(
          context,
          inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
        )
        .setWorkerFactory(factory)
        .build()
    val result = worker.doWork()
    assertThat(result).isEqualTo(Result.retry())
  }
}
