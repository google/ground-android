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
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.data.sync.SurveySyncWorker.Companion.MAX_SYNC_ATTEMPTS
import org.groundplatform.android.data.sync.SurveySyncWorker.Companion.SURVEY_ID_PARAM_KEY
import org.groundplatform.android.data.sync.SurveySyncWorker.Companion.SYNC_TIMEOUT_MILLIS
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.usecases.survey.SyncSurveyUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySyncWorkerTest : BaseHiltTest() {
  private lateinit var context: Context
  @Mock lateinit var syncSurvey: SyncSurveyUseCase
  @Mock lateinit var surveyRepository: SurveyRepositoryInterface

  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

  private val factory =
    object : WorkerFactory() {
      override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
      ) = SurveySyncWorker(appContext, workerParameters, syncSurvey, surveyRepository, ioDispatcher)
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
    `when`(surveyRepository.getOfflineSurvey(SURVEY.id)).thenReturn(SURVEY)
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
    verify(syncSurvey).invoke(SURVEY.id)
  }

  @Test
  fun `doWork() retries on failure`() = runWithTestDispatcher {
    `when`(surveyRepository.getOfflineSurvey(SURVEY.id)).thenReturn(SURVEY)
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
  fun `doWork() retries if the max number of attempts was not yet reached`() =
    runWithTestDispatcher {
      `when`(surveyRepository.getOfflineSurvey(SURVEY.id)).thenReturn(SURVEY)
      `when`(syncSurvey(SURVEY.id)).thenThrow(NotFoundException())

      val worker =
        TestListenableWorkerBuilder<SurveySyncWorker>(
            context,
            inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
          )
          .setWorkerFactory(factory)
          .setRunAttemptCount(MAX_SYNC_ATTEMPTS - 2)
          .build()
      val result = worker.doWork()
      assertThat(result).isEqualTo(Result.retry())
    }

  @Test
  fun `doWork() gives up without syncing once the max number of attempts is reached`() =
    runWithTestDispatcher {
      val worker =
        TestListenableWorkerBuilder<SurveySyncWorker>(
            context,
            inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
          )
          .setWorkerFactory(factory)
          .setRunAttemptCount(MAX_SYNC_ATTEMPTS)
          .build()
      val result = worker.doWork()

      // Nothing may be read from remote once the attempts are used up.
      assertThat(result).isEqualTo(Result.failure())
      verifyBlocking(syncSurvey, never()) { invoke(SURVEY.id) }
    }

  @Test
  fun `doWork() gives up when the sync fails on the final attempt`() = runWithTestDispatcher {
    `when`(surveyRepository.getOfflineSurvey(SURVEY.id)).thenReturn(SURVEY)
    `when`(syncSurvey(SURVEY.id)).thenThrow(NotFoundException())

    val worker =
      TestListenableWorkerBuilder<SurveySyncWorker>(
          context,
          inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
        )
        .setWorkerFactory(factory)
        .setRunAttemptCount(MAX_SYNC_ATTEMPTS - 1)
        .build()
    val result = worker.doWork()
    assertThat(result).isEqualTo(Result.failure())
  }

  @Test
  fun `doWork() retries when a sync exceeds the timeout`() = runWithTestDispatcher {
    `when`(surveyRepository.getOfflineSurvey(SURVEY.id)).thenReturn(SURVEY)
    syncSurvey.stub {
      onBlocking { invoke(SURVEY.id) }
        .doSuspendableAnswer {
          delay((SYNC_TIMEOUT_MILLIS * 2).milliseconds)
          SURVEY
        }
    }

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
  fun `doWork() retries when the sync times out`() = runWithTestDispatcher {
    val timeoutCancellationException =
      try {
        withTimeout(1.milliseconds) { delay(Long.MAX_VALUE.milliseconds) }
        error("withTimeout should have timed out")
      } catch (e: TimeoutCancellationException) {
        e
      }
    `when`(surveyRepository.getOfflineSurvey(SURVEY.id)).thenReturn(SURVEY)
    `when`(syncSurvey(SURVEY.id)).thenThrow(timeoutCancellationException)

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
  fun `doWork() rethrows when WorkManager stops the worker`() = runWithTestDispatcher {
    `when`(surveyRepository.getOfflineSurvey(SURVEY.id)).thenReturn(SURVEY)
    `when`(syncSurvey(SURVEY.id)).thenThrow(CancellationException("Stopped"))

    val worker =
      TestListenableWorkerBuilder<SurveySyncWorker>(
          context,
          inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
        )
        .setWorkerFactory(factory)
        .build()
    assertFailsWith<CancellationException> { worker.doWork() }
  }

  @Test
  fun `doWork() skips sync and unsubscribes when survey is no longer available offline`() =
    runWithTestDispatcher {
      `when`(surveyRepository.getOfflineSurvey(SURVEY.id)).thenReturn(null)

      val worker =
        TestListenableWorkerBuilder<SurveySyncWorker>(
            context,
            inputData = workDataOf(Pair(SURVEY_ID_PARAM_KEY, SURVEY.id)),
          )
          .setWorkerFactory(factory)
          .build()
      val result = worker.doWork()

      assertThat(result).isEqualTo(Result.success())
      verifyBlocking(syncSurvey, never()) { invoke(SURVEY.id) }
      verify(surveyRepository).unsubscribeFromSurveyUpdates(SURVEY.id)
    }
}
