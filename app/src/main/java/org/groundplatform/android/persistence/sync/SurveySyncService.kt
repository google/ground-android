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

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import java.util.UUID
import javax.inject.Inject
import timber.log.Timber

/** Service responsible for enqueuing survey and LOI updates from remote server. */
class SurveySyncService @Inject constructor(private val workManager: WorkManager) {

  /**
   * Enqueues a worker that fetches the latest survey and LOIs from the remote survey and updates
   * the local db accordingly.
   *
   * @return The id of the worker request, used in tests to retrieve the worker status.
   */
  fun enqueueSync(surveyId: String): UUID {
    val inputData = SurveySyncWorker.createInputData(surveyId)
    val request =
      WorkRequestBuilder()
        .setWorkerClass(SurveySyncWorker::class.java)
        .buildWorkerRequest(inputData)
    workManager
      .enqueueUniqueWork(
        "${SurveySyncWorker::class.java}#${surveyId}",
        ExistingWorkPolicy.APPEND,
        request,
      )
      .result
      .get()
    Timber.d("Survey sync enqueued for $surveyId")
    return request.id
  }
}
