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
package com.google.android.ground.model.locationofinterest

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus

/** Alias for a map of properties with string names. */
typealias LoiProperties = Map<String, Any>

const val LOI_NAME_PROPERTY = "name"

fun generateProperties(loiName: String? = null): LoiProperties =
  loiName?.let { mapOf(LOI_NAME_PROPERTY to it) } ?: mapOf()

/** User-defined locations of interest (LOI) shown on the map. */
data class LocationOfInterest(
  /** A system-defined ID for this LOI. */
  val id: String,
  /** The survey ID associated with this LOI. */
  val surveyId: String,
  /** The job associated with this LOI. */
  val job: Job,
  /** A user-specified ID for this location of interest. */
  val customId: String = "",
  /** User and time audit info pertaining to the creation of this LOI. */
  val created: AuditInfo,
  /** User and time audit info pertaining to the last modification of this LOI. */
  val lastModified: AuditInfo,
  /** Geometry associated with this LOI. */
  val geometry: Geometry,
  /** The number of submissions that have been made for this LOI. */
  val submissionCount: Int = 0,
  /** Custom map of properties for this LOI. Corresponds to the properties field in GeoJSON */
  val properties: LoiProperties = mapOf(),
  /** Whether the LOI was predefined in the survey or not (i.e. added ad hoc). */
  val isPredefined: Boolean? = null,
) {

  /**
   * Converts this LOI to a mutation that can be used to update this LOI in the remote and local
   * database.
   */
  // TODO: Remove this test-only method
  // Issue URL: https://github.com/google/ground-android/issues/2903
  fun toMutation(type: Mutation.Type, userId: String): LocationOfInterestMutation =
    LocationOfInterestMutation(
      jobId = job.id,
      type = type,
      syncStatus = SyncStatus.PENDING,
      surveyId = surveyId,
      locationOfInterestId = id,
      userId = userId,
      customId = customId,
      clientTimestamp = lastModified.clientTimestamp,
      geometry = geometry,
      submissionCount = submissionCount,
      properties = properties,
      isPredefined = isPredefined,
      collectionId = "",
    )

  fun getProperty(key: String): String = properties[key]?.toString() ?: ""
}
