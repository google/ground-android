/*
 * Copyright 2025 Google LLC
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

package org.groundplatform.android.ui.home.mapcontainer.jobs

import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.locationofinterest.LocationOfInterest

/** Data classes used to populate the data collection entry UI, like the LOI bottom sheet. */
sealed interface DataCollectionEntryPointData {
  val canCollectData: Boolean
}

data class SelectedLoiSheetData(
  override val canCollectData: Boolean,
  val loi: LocationOfInterest,
  val submissionCount: Int,
  val showDeleteLoiButton: Boolean,
) : DataCollectionEntryPointData

data class AdHocDataCollectionButtonData(override val canCollectData: Boolean, val job: Job) :
  DataCollectionEntryPointData
