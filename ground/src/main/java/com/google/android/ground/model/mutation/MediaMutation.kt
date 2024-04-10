/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.model.mutation

import java.util.Date

/**
 * Represents a local change to media associated with a submission.
 *
 * These mutations are directly dependent upon another mutation type: SubmissionMutation. At
 * present, any MediaMutation will only have relevance in the context of a submission. To this end,
 * several properties are derived from the mutation's associated submission mutation.
 */
data class MediaMutation(
  override val type: Type,
  val mediaId: String,
  val submissionMutation: SubmissionMutation,
  override val id: Long? = null,
  override val syncStatus: SyncStatus = SyncStatus.PENDING,
  override val retryCount: Long = 0,
  override val lastError: String = "",
) : Mutation() {
  override val surveyId: String = submissionMutation.surveyId
  override val locationOfInterestId: String = submissionMutation.locationOfInterestId
  override val clientTimestamp: Date = submissionMutation.clientTimestamp
  override val userId: String = submissionMutation.userId
}
