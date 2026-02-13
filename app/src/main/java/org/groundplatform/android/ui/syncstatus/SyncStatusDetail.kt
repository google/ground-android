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
package org.groundplatform.android.ui.syncstatus

import java.util.Date
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.submission.UploadQueueEntry

/**
 * Defines the set of data needed to display the human-readable status of a queued
 * [UploadQueueEntry].
 */
data class SyncStatusDetail(
  /** The username of the user who made this change. */
  val user: String,
  /** The underlying status of an upload. */
  val status: Mutation.SyncStatus,
  /** The instant at which this change was initiated on the client device. */
  val timestamp: Date,
  /** A human-readable label summarizing what data changed. */
  val label: String,
  /** A human-readable label providing further information on the change. */
  val subtitle: String,
  /** A description of the survey. */
  val description: String = "",
)
