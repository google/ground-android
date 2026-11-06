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

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random
import timber.log.Timber

/** Service responsible for enqueuing survey and LOI updates from remote server. */
class SurveySyncService @Inject constructor(private val workManager: WorkManager) {

  /**
   * Enqueues the workers that refresh a survey from the remote server: a quick one for the survey
   * definition (a single document read) and a delayed one for its LOIs, which is where the reads
   * add up.
   *
   * @return The id of the LOI sync request, used in tests to retrieve the worker status.
   */
  fun enqueueSync(surveyId: String): UUID {
    enqueue(surveyId, definitionOnly = true, delayMillis = DEFINITION_SYNC_DELAY_MILLIS)
    val loiRequestId = enqueue(surveyId, definitionOnly = false, delayMillis = loiSyncDelayMillis())
    Timber.d("Survey sync enqueued for $surveyId")
    return loiRequestId
  }

  private fun enqueue(surveyId: String, definitionOnly: Boolean, delayMillis: Long): UUID {
    val request =
      WorkRequestBuilder()
        .setWorkerClass(SurveySyncWorker::class.java)
        .setInitialDelay(delayMillis)
        .buildWorkerRequest(SurveySyncWorker.createInputData(surveyId, definitionOnly))
    val suffix = if (definitionOnly) "#definition" else ""
    workManager
      .enqueueUniqueWork(
        "${SurveySyncWorker::class.java}#${surveyId}$suffix",
        ExistingWorkPolicy.KEEP,
        request,
      )
      .result
      .get()
    return request.id
  }

  /** Spreads devices notified at the same time so they don't all read from Firestore at once. */
  private fun loiSyncDelayMillis() = LOI_SYNC_DELAY_MILLIS + Random.nextLong(LOI_SYNC_JITTER_MILLIS)

  companion object {
    /** Lets a burst of changes settle before refreshing the survey definition. */
    private const val DEFINITION_SYNC_DELAY_MILLIS = 10_000L

    /**
     * How long to wait before syncing LOIs. Each run reads LOIs from Firestore and every change
     * notifies every device on the survey, so this is kept long to bound reads when many data
     * collectors share a survey.
     */
    private const val LOI_SYNC_DELAY_MILLIS = 30 * 60 * 1000L

    /** Upper bound of the random delay added to [LOI_SYNC_DELAY_MILLIS]. */
    private const val LOI_SYNC_JITTER_MILLIS = 5 * 60 * 1000L
  }
}
