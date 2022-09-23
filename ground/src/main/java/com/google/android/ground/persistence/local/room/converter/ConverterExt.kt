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
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.ResponseMap
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.entity.*
import com.google.android.ground.persistence.local.room.models.*
import com.google.android.ground.persistence.local.room.relations.SurveyEntityAndRelations
import com.google.android.ground.persistence.local.room.relations.TaskEntityAndRelations
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import java.net.MalformedURLException
import java.util.*
import kotlinx.collections.immutable.toPersistentList
import org.json.JSONObject
import timber.log.Timber

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

@Throws(LocalDataConsistencyException::class)
fun SubmissionMutationEntity.toSubmissionMutation(survey: Survey): SubmissionMutation {
  val job =
    survey.getJob(jobId).orElseThrow {
      LocalDataConsistencyException("Unknown jobId in submission mutation $id")
    }

  return SubmissionMutation(
    job = job,
    submissionId = submissionId,
    responseDeltas = ResponseDeltasConverter.fromString(job, responseDeltas),
    id = id,
    surveyId = surveyId,
    locationOfInterestId = locationOfInterestId,
    type = type.toMutationType(),
    syncStatus = syncStatus.toMutationSyncStatus(),
    retryCount = retryCount,
    lastError = lastError,
    userId = userId,
    clientTimestamp = Date(clientTimestamp)
  )
}

fun SubmissionMutation.toSubmissionMutationEntity() =
  SubmissionMutationEntity(
    id = id,
    surveyId = surveyId,
    locationOfInterestId = locationOfInterestId,
    jobId = job!!.id,
    submissionId = submissionId,
    type = MutationEntityType.fromMutationType(type),
    syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(syncStatus),
    responseDeltas = ResponseDeltasConverter.toString(responseDeltas),
    retryCount = retryCount,
    lastError = lastError,
    userId = userId,
    clientTimestamp = clientTimestamp.time
  )

fun SurveyEntityAndRelations.toSurvey(): Survey {
  val jobMap = ImmutableMap.builder<String, Job>()
  val baseMaps = ImmutableList.builder<BaseMap>()

  for (jobEntityAndRelations in jobEntityAndRelations) {
    val job = JobConverter.convertFromDataStoreObjectWithRelations(jobEntityAndRelations!!)
    jobMap.put(job.id, job)
  }
  for (source in baseMapEntityAndRelations) {
    try {
      baseMaps.add(BaseMapConverter.convertFromDataStoreObject(source))
    } catch (e: MalformedURLException) {
      Timber.d("Skipping basemap source with malformed URL %s", source.url)
    }
  }
  val surveyEntity = surveyEntity

  return Survey(
    surveyEntity.id,
    surveyEntity.title!!,
    surveyEntity.description!!,
    jobMap.build(),
    baseMaps.build(),
    surveyEntity.acl.toStringMap()
  )
}

private fun JSONObject?.toStringMap(): ImmutableMap<String, String> {
  val builder: ImmutableMap.Builder<String, String> = ImmutableMap.builder()
  val keys = this!!.keys()

  while (keys.hasNext()) {
    val key = keys.next()
    val value = this.optString(key, null.toString())
    builder.put(key, value)
  }

  return builder.build()
}

fun Survey.toSurveyEntity() =
  SurveyEntity(
    id = id,
    title = title,
    description = description,
    acl = JSONObject(acl as Map<*, *>?)
  )

fun Task.toLocalDataStoreObject(jobId: String?) =
  TaskEntity(
    id = id,
    jobId = jobId,
    index = index,
    label = label,
    isRequired = isRequired,
    taskType = TaskEntityType.fromTaskType(type)
  )

fun TaskEntityAndRelations.toModelObject(): Task {
  var multipleChoice: MultipleChoice? = null

  if (multipleChoiceEntities.isNotEmpty()) {
    if (multipleChoiceEntities.size > 1) {
      Timber.e("More than 1 multiple choice found for task")
    }

    multipleChoice = multipleChoiceEntities[0].toMultipleChoice(optionEntities)
  }

  return Task(
    taskEntity.id,
    taskEntity.index,
    taskEntity.taskType.toTaskType(),
    taskEntity.label!!,
    taskEntity.isRequired,
    multipleChoice
  )
}

private fun TileSetEntityState.toModelObject() =
  when (this) {
    TileSetEntityState.PENDING -> TileSet.State.PENDING
    TileSetEntityState.IN_PROGRESS -> TileSet.State.IN_PROGRESS
    TileSetEntityState.DOWNLOADED -> TileSet.State.DOWNLOADED
    TileSetEntityState.FAILED -> TileSet.State.FAILED
    else -> throw IllegalArgumentException("Unknown tile source state: $this")
  }

private fun TileSet.State.toLocalDataStoreObject() =
  when (this) {
    TileSet.State.PENDING -> TileSetEntityState.PENDING
    TileSet.State.IN_PROGRESS -> TileSetEntityState.IN_PROGRESS
    TileSet.State.FAILED -> TileSetEntityState.FAILED
    TileSet.State.DOWNLOADED -> TileSetEntityState.DOWNLOADED
  }

fun TileSetEntity.toModelObject() =
  TileSet(
    id = id,
    url = url,
    path = path,
    offlineAreaReferenceCount = offlineAreaReferenceCount,
    state = state.toModelObject()
  )

fun TileSet.toLocalDataStoreObject() =
  TileSetEntity(
    id = id,
    url = url,
    path = path,
    offlineAreaReferenceCount = offlineAreaReferenceCount,
    state = state.toLocalDataStoreObject()
  )

fun User.toLocalDataStoreObject() =
  UserEntity(id = id, email = email, displayName = displayName, photoUrl = photoUrl)

fun UserEntity.toModelObject() =
  User(id = id, email = email, displayName = displayName, photoUrl = photoUrl)
