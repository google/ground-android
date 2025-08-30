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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker.Result.failure
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.usecases.survey.SyncSurveyUseCase
import timber.log.Timber

/** Worker responsible for syncing latest surveys and LOIs from remote server to local db. */
@HiltWorker
class SurveySyncWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val syncSurvey: SyncSurveyUseCase,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, params) {
  private val surveyId: String? = params.inputData.getString(SURVEY_ID_PARAM_KEY)

  override suspend fun doWork(): Result = withContext(ioDispatcher) { doWorkInternal() }

  private suspend fun doWorkInternal(): Result {
    if (surveyId == null) {
      Timber.e("Survey sync scheduled with null surveyId")
      return failure()
    }

    try {
      Timber.d("Syncing survey $surveyId")
      syncSurvey(surveyId)
      return success()
    } catch (t: Throwable) {
      Timber.e(t, "Failed to sync survey $surveyId, retrying")
      return retry()
    }
  }

  companion object {
    /** The key in worker input data containing the id of the survey to be synced. */
    internal const val SURVEY_ID_PARAM_KEY = "surveyId"

    /** Returns a new work [Data] object containing the specified survey id. */
    fun createInputData(surveyId: String): Data =
      Data.Builder().putString(SURVEY_ID_PARAM_KEY, surveyId).build()
  }
}
