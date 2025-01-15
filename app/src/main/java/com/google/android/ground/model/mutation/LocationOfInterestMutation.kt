/*
 * Copyright 2019 Google LLC
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

import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.locationofinterest.LoiProperties
import java.util.Date

data class LocationOfInterestMutation(
  override val id: Long? = null,
  override val type: Type = Type.UNKNOWN,
  override val syncStatus: SyncStatus = SyncStatus.UNKNOWN,
  override val surveyId: String = "",
  override val locationOfInterestId: String = "",
  override val userId: String = "",
  override val clientTimestamp: Date = Date(),
  override val retryCount: Long = 0,
  override val lastError: String = "",
  override val collectionId: String,
  val jobId: String = "",
  val customId: String = "",
  val geometry: Geometry? = null,
  val submissionCount: Int = 0,
  val properties: LoiProperties = mapOf(),
  val isPredefined: Boolean? = null,
) : Mutation()
