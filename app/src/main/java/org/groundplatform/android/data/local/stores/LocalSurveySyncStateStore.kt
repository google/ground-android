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
package org.groundplatform.android.data.local.stores

import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveySyncState

/**
 * Provides access to [SurveySyncState] data in local storage.
 *
 * This keeps track of the latest LOI server timestamp that has been synced for a survey so a later
 * sync can fetch only data that changed after that point.
 */
interface LocalSurveySyncStateStore {
  /** Returns the sync state of the given survey, or null if it has never been fully synced. */
  suspend fun get(surveyId: String): SurveySyncState?

  /**
   * Records the latest LOI server timestamp for an incremental sync without changing the rest of
   * the survey's sync state. Does nothing if the survey has never been fully synced.
   */
  suspend fun recordIncrementalSync(
    surveyId: String,
    latestLoiServerTimestamp: Long,
  )

  /**
   * Records that the survey's LOIs were fully reconciled and replaces any existing state for it.
   * [latestLoiServerTimestamp] becomes the latest server timestamp used for later incremental
   * syncs, and [dataVisibility] stores the visibility used to fetch the data so later changes can
   * be detected.
   */
  suspend fun recordFullSync(
    surveyId: String,
    latestLoiServerTimestamp: Long,
    dataVisibility: Survey.DataVisibility?,
  )
}
