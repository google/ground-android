/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.syncstatus

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.UploadQueueEntry
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.LocationOfInterestHelper
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Provides data for the [SyncStatusFragment] UI.
 *
 * This model retrieves the current upload queue from the mutations repository and converts them to
 * [SyncStatusDetail] to prepare the data for display. It assumes that any filtering or
 * transformation of the underlying mutation queue is done before fetching at the repository level.
 */
class SyncStatusViewModel
@Inject
internal constructor(
  mutationRepository: MutationRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val userRepository: UserRepository,
  private val locationOfInterestHelper: LocationOfInterestHelper,
  private val surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  /**
   * A complete list of [SyncStatusDetail] indicating the current status of local changes being
   * synced to remote servers.
   */
  internal val uploadStatus: LiveData<List<SyncStatusDetail>> =
    mutationRepository
      .getUploadQueueFlow()
      .map { it.mapNotNull { upload -> toSyncStatusDetail(upload) } }
      .asLiveData()

  private suspend fun toSyncStatusDetail(uploadQueueEntry: UploadQueueEntry): SyncStatusDetail? {
    val mutation =
      uploadQueueEntry.loiMutation ?: uploadQueueEntry.submissionMutation ?: return null
    val user = userRepository.getUser(uploadQueueEntry.userId)
    val timestamp = uploadQueueEntry.clientTimestamp
    val status = uploadQueueEntry.uploadStatus

    return when (mutation) {
      is LocationOfInterestMutation -> {
        val loi =
          locationOfInterestRepository.getOfflineLoi(
            mutation.surveyId,
            mutation.locationOfInterestId,
          )
        if (loi == null) {
          Timber.e("LOI not found for mutation $mutation")
          null
        } else {
          SyncStatusDetail(
            user = user.displayName,
            timestamp = timestamp,
            status = status,
            label = locationOfInterestHelper.getJobName(loi) ?: "",
            subtitle = locationOfInterestHelper.getDisplayLoiName(loi),
          )
        }
      }

      is SubmissionMutation -> {
        SyncStatusDetail(
          user = user.displayName,
          timestamp = timestamp,
          status = status,
          label = mutation.job.name ?: "",
          subtitle = surveyRepository.getOfflineSurvey(mutation.surveyId)?.title ?: "",
        )
      }
    }
  }
}
