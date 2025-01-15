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

package com.google.android.ground.model.submission

import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import java.util.Date

/**
 * A set of changes to be applied to the remote datastore, initiated by the user by completing the
 * data collection flow and clicking "Submit".
 */
data class UploadQueueEntry(
  val userId: String,
  val clientTimestamp: Date,
  val uploadStatus: Mutation.SyncStatus,
  val loiMutation: LocationOfInterestMutation?,
  val submissionMutation: SubmissionMutation?,
) {
  fun mutations(): List<Mutation> = listOfNotNull(loiMutation, submissionMutation)
}
