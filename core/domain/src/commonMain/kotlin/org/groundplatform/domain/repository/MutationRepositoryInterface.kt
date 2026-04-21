/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.repository

import kotlinx.coroutines.flow.Flow
import org.groundplatform.domain.model.mutation.LocationOfInterestMutation
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.model.mutation.SubmissionMutation
import org.groundplatform.domain.model.submission.UploadQueueEntry

/**
 * Coordinates persistence of mutations across [LocationOfInterestMutation] and [SubmissionMutation]
 * local data stores.
 */
interface MutationRepositoryInterface {
  /**
   * Return the set of data upload queue entries not yet marked as completed sorted in chronological
   * order (FIFO). Media/photo uploads are not included.
   */
  suspend fun getIncompleteUploads(): List<UploadQueueEntry>

  /**
   * Return the set of photo/media upload queue entries not yet marked as completed, sorted in
   * chronological order (FIFO).
   */
  suspend fun getIncompleteMediaMutations(): List<SubmissionMutation>

  /**
   * Returns a [Flow] which emits the upload queue once and on each change, sorted in chronological
   * order (FIFO).
   */
  fun getUploadQueueFlow(): Flow<List<UploadQueueEntry>>

  /**
   * Mark pending mutations as ready for media upload. If the mutation is of type DELETE, also
   * removes the corresponding submission or LOI.
   */
  suspend fun finalizePendingMutationsForMediaUpload(mutations: List<Mutation>)

  suspend fun markAsInProgress(mutations: List<Mutation>)

  suspend fun uploadMutations(mutations: List<Mutation>)

  suspend fun markAsComplete(mutations: List<Mutation>)

  suspend fun markAsFailed(mutations: List<Mutation>, error: Throwable)

  suspend fun markAsMediaUploadInProgress(mutations: List<SubmissionMutation>)

  suspend fun markAsFailedMediaUpload(mutations: List<SubmissionMutation>, error: Throwable)
}
