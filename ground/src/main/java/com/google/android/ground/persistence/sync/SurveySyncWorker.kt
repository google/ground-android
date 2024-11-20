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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.android.ground.Config.MAX_SYNC_WORKER_RETRY_ATTEMPTS
import com.google.android.ground.FirebaseCrashLogger
import com.google.android.ground.domain.usecases.survey.SyncSurveyUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Worker responsible for syncing latest surveys and LOIs from remote server to local db. */
@HiltWorker
class SurveySyncWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val syncSurvey: SyncSurveyUseCase,
) : CoroutineWorker(context, params) {
  private val surveyId: String? = params.inputData.getString(SURVEY_ID_PARAM_KEY)

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) { doWorkInternal() }

  private suspend fun doWorkInternal(): Result {
    if (surveyId == null) {
      Timber.e("Survey sync scheduled with null surveyId")
      return Result.failure()
    }

    try {
      Timber.d("Syncing survey $surveyId")
      syncSurvey(surveyId)
    } catch (t: Throwable) {
      val logger = FirebaseCrashLogger()
      logger.setSelectedSurveyId(surveyId)
      logger.logException(t)
      return if (this.runAttemptCount > MAX_SYNC_WORKER_RETRY_ATTEMPTS) {
        Timber.v(t, "Survey sync failed too many times. Giving up.")
        Result.failure()
      } else {
        Timber.v(t, "Survey sync. Retrying...")
        Result.retry()
      }
    }

    return Result.success()
  }

  companion object {
    /** The key in worker input data containing the id of the survey to be synced. */
    internal const val SURVEY_ID_PARAM_KEY = "surveyId"

    /** Returns a new work [Data] object containing the specified survey id. */
    fun createInputData(surveyId: String): Data =
      Data.Builder().putString(SURVEY_ID_PARAM_KEY, surveyId).build()
  }
}
