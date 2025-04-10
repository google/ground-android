/*
 * Copyright 2018 Google LLC
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
package org.groundplatform.android.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.groundplatform.android.model.submission.DraftSubmission
import org.groundplatform.android.persistence.sync.MediaUploadWorkManager
import org.groundplatform.android.persistence.sync.MutationSyncWorkManager
import org.groundplatform.android.repository.MutationRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.common.SharedViewModel
import timber.log.Timber

private const val AWAITING_PHOTO_CAPTURE_KEY = "awaiting_photo_capture"

@SharedViewModel
class HomeScreenViewModel
@Inject
internal constructor(
  private val offlineAreaRepository: OfflineAreaRepository,
  private val submissionRepository: SubmissionRepository,
  private val mutationRepository: MutationRepository,
  private val mutationSyncWorkManager: MutationSyncWorkManager,
  private val mediaUploadWorkManager: MediaUploadWorkManager,
  val surveyRepository: SurveyRepository,
  val userRepository: UserRepository,
) : AbstractViewModel() {

  private val savedStateHandle: SavedStateHandle = SavedStateHandle()
  private val _openDrawerRequests: MutableSharedFlow<Unit> = MutableSharedFlow()
  val openDrawerRequestsFlow: SharedFlow<Unit> = _openDrawerRequests.asSharedFlow()

  // TODO: Allow tile source configuration from a non-survey accessible source.
  // Issue URL: https://github.com/google/ground-android/issues/1730
  val showOfflineAreaMenuItem: LiveData<Boolean> = MutableLiveData(true)

  /* Indicates the application is being restored after a photo capture.
   *
   * We need to persist this state here to control [HomeScreenFragement] UI treatments when returning
   * from a photo capture task—we do it this way because saving instance state bundles across fragments
   * does not prove simple.
   * */
  var awaitingPhotoCapture: Boolean
    get() = savedStateHandle[AWAITING_PHOTO_CAPTURE_KEY] ?: false
    set(newValue) {
      savedStateHandle[AWAITING_PHOTO_CAPTURE_KEY] = newValue
    }

  init {
    viewModelScope.launch { kickLocalMutationSyncWorkers() }
  }

  /**
   * Enqueue data and photo upload workers for all pending mutations when home screen is first
   * opened as a workaround the get stuck mutations (i.e., PENDING or FAILED mutations with no
   * scheduled workers) going again. If there are no mutations in the upload queue this will be a
   * no-op. Workaround for https://github.com/google/ground-android/issues/2751.
   */
  private suspend fun kickLocalMutationSyncWorkers() {
    if (mutationRepository.getIncompleteUploads().isNotEmpty()) {
      mutationSyncWorkManager.enqueueSyncWorker()
    }
    if (mutationRepository.getIncompleteMediaMutations().isNotEmpty()) {
      mediaUploadWorkManager.enqueueSyncWorker()
    }
  }

  /** Attempts to return draft submission for the currently active survey. */
  suspend fun getDraftSubmission(): DraftSubmission? {
    val draftId = submissionRepository.getDraftSubmissionsId()
    val survey = surveyRepository.activeSurvey

    if (draftId.isEmpty() || survey == null) {
      // Missing draft submission
      return null
    }

    val draft = submissionRepository.getDraftSubmission(draftId, survey) ?: return null

    if (draft.surveyId != survey.id) {
      Timber.e("Skipping draft submission, survey id doesn't match")
      return null
    }

    // TODO: Check whether the previous user id matches with current user or not.
    // Issue URL: https://github.com/google/ground-android/issues/2903
    return draft
  }

  fun openNavDrawer() {
    viewModelScope.launch { _openDrawerRequests.emit(Unit) }
  }

  suspend fun getOfflineAreas() = offlineAreaRepository.offlineAreas().first()

  fun signOut() {
    viewModelScope.launch { userRepository.signOut() }
  }
}
