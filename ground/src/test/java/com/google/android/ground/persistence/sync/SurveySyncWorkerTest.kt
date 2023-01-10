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
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestWorkerBuilder
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.repository.SurveyRepository
import com.sharedtest.FakeData
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Single
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executors.newSingleThreadExecutor
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySyncWorkerTest : BaseHiltTest() {
  @Mock
  private lateinit var surveyRepository: SurveyRepository

  @Inject
  @ApplicationContext
  lateinit var context: Context

  @Before
  override fun setUp() {
    super.setUp()
    `when`(surveyRepository.syncSurveyWithRemote(anyString()))
      .thenReturn(Single.just(FakeData.SURVEY))
  }

  @Test
  fun testCallSyncSurveyWithSpecifiedId() {
    val surveyId = "testSurveyId"
    val worker = createWorker(SurveySyncWorker.createInputData(surveyId))

    val result = worker.doWork()

    verify(surveyRepository).syncSurveyWithRemote(surveyId)
    assertThat(result, `is`(Result.success()))
  }

  private fun createWorker(inputData: Data): SurveySyncWorker =
    TestWorkerBuilder<SurveySyncWorker>(
      context, newSingleThreadExecutor(), inputData
    )
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
}
