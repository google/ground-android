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

package org.groundplatform.android.data.sync

import android.content.Context
import android.content.res.Resources.NotFoundException
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.sync.SurveySyncWorker.Companion.SURVEY_ID_PARAM_KEY
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.domain.usecases.survey.SyncSurveyUseCase
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
  @Mock lateinit var syncSurvey: SyncSurveyUseCase

  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
  @Inject lateinit var localValueStore: LocalValueStore

  private val factory =
    object : WorkerFactory() {
      override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
      ) = SurveySyncWorker(appContext, workerParameters, syncSurvey, localValueStore, ioDispatcher)
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

  @Test
  fun `doWork() clears the survey's stale pref on success`() = runWithTestDispatcher {
    `when`(syncSurvey(SURVEY.id)).thenReturn(SURVEY)
    localValueStore.staleSurveyIds = setOf(SURVEY.id, OTHER_SURVEY_ID)

    val worker =
      TestListenableWorkerBuilder<SurveySyncWorker>(
          context,
          inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
        )
        .setWorkerFactory(factory)
        .build()
    worker.doWork()

    // Other surveys' markers must survive; only the one just synced is cleared.
    assertThat(localValueStore.staleSurveyIds).containsExactly(OTHER_SURVEY_ID)
  }

  @Test
  fun `doWork() keeps the survey's stale pref when sync fails`() = runWithTestDispatcher {
    `when`(syncSurvey(SURVEY.id)).thenThrow(NotFoundException())
    localValueStore.staleSurveyIds = setOf(SURVEY.id)

    val worker =
      TestListenableWorkerBuilder<SurveySyncWorker>(
          context,
          inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
        )
        .setWorkerFactory(factory)
        .build()
    worker.doWork()

    assertThat(localValueStore.staleSurveyIds).containsExactly(SURVEY.id)
  }

  private companion object {
    const val OTHER_SURVEY_ID = "other-survey-id"
  }
}
