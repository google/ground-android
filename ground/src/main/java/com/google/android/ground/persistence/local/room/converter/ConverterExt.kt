/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.persistence.local.room.converter

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.ResponseMap
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.entity.MultipleChoiceEntity
import com.google.android.ground.persistence.local.room.entity.OfflineAreaEntity
import com.google.android.ground.persistence.local.room.entity.OptionEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionEntity
import com.google.android.ground.persistence.local.room.models.EntityState
import com.google.android.ground.persistence.local.room.models.MultipleChoiceEntityType
import com.google.android.ground.persistence.local.room.models.OfflineAreaEntityState
import com.google.common.collect.ImmutableList
import kotlinx.collections.immutable.toPersistentList

fun MultipleChoiceEntity.toMultipleChoice(optionEntities: List<OptionEntity>): MultipleChoice {
  val listBuilder = ImmutableList.builder<Option>()

  for (optionEntity in optionEntities) {
    listBuilder.add(optionEntity.toOption())
  }

  return MultipleChoice(listBuilder.build().toPersistentList(), this.type.toCardinality())
}

fun MultipleChoice.toMultipleChoiceEntity(taskId: String): MultipleChoiceEntity =
  MultipleChoiceEntity(taskId, MultipleChoiceEntityType.fromCardinality(this.cardinality))

private fun OfflineAreaEntityState.toOfflineAreaState() =
  when (this) {
    OfflineAreaEntityState.PENDING -> OfflineArea.State.PENDING
    OfflineAreaEntityState.IN_PROGRESS -> OfflineArea.State.IN_PROGRESS
    OfflineAreaEntityState.DOWNLOADED -> OfflineArea.State.DOWNLOADED
    OfflineAreaEntityState.FAILED -> OfflineArea.State.FAILED
    else -> throw IllegalArgumentException("Unknown area state: $this")
  }

private fun OfflineArea.State.toOfflineAreaEntityState() =
  when (this) {
    OfflineArea.State.PENDING -> OfflineAreaEntityState.PENDING
    OfflineArea.State.IN_PROGRESS -> OfflineAreaEntityState.IN_PROGRESS
    OfflineArea.State.FAILED -> OfflineAreaEntityState.FAILED
    OfflineArea.State.DOWNLOADED -> OfflineAreaEntityState.DOWNLOADED
  }

fun OfflineArea.toOfflineAreaEntity() =
  OfflineAreaEntity(
    id = this.id,
    state = this.state.toOfflineAreaEntityState(),
    name = this.name,
    north = this.bounds.northeast.latitude,
    east = this.bounds.northeast.longitude,
    south = this.bounds.southwest.latitude,
    west = this.bounds.southwest.longitude
  )

fun OfflineAreaEntity.toOfflineArea(): OfflineArea {
  val northEast = LatLng(this.north, this.east)
  val southWest = LatLng(this.south, this.west)
  val bounds = LatLngBounds(southWest, northEast)

  return OfflineArea(this.id, this.state.toOfflineAreaState(), bounds, this.name)
}

fun Option.toOptionEntity(taskId: String) =
  OptionEntity(id = this.id, code = this.code, label = this.label, taskId = taskId)

fun OptionEntity.toOption() = Option(id = this.id, code = this.code, label = this.label)

fun SubmissionEntity.toSubmission(loi: LocationOfInterest): Submission {
  val jobId = this.jobId
  val job = loi.job

  if (job.id != jobId) {
    throw LocalDataConsistencyException(
      "LOI job id ${job.id} does not match submission ${this.jobId}"
    )
  }

  return Submission(
    id = this.id,
    surveyId = loi.surveyId,
    locationOfInterest = loi,
    job = job,
    created = AuditInfoConverter.convertFromDataStoreObject(this.created),
    lastModified = AuditInfoConverter.convertFromDataStoreObject(this.lastModified),
    responses = ResponseMapConverter.fromString(job, this.responses)
  )
}

fun Submission.toSubmissionEntity() =
  SubmissionEntity(
    id = this.id,
    jobId = this.job.id,
    locationOfInterestId = this.locationOfInterest.id,
    state = EntityState.DEFAULT,
    responses = ResponseMapConverter.toString(this.responses),
    created = AuditInfoConverter.convertToDataStoreObject(this.created),
    lastModified = AuditInfoConverter.convertToDataStoreObject(this.lastModified)
  )

fun SubmissionMutation.toSubmissionEntity(created: AuditInfo): SubmissionEntity {
  val authInfo = AuditInfoConverter.convertToDataStoreObject(created)

  return SubmissionEntity(
    id = this.submissionId,
    jobId = this.job!!.id,
    locationOfInterestId = this.locationOfInterestId,
    state = EntityState.DEFAULT,
    responses = ResponseMapConverter.toString(ResponseMap().copyWithDeltas(this.responseDeltas)),
    created = authInfo,
    lastModified = authInfo
  )
}
