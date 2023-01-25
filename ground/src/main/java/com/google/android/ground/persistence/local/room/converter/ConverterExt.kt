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
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskDataMap
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.entity.*
import com.google.android.ground.persistence.local.room.models.*
import com.google.android.ground.persistence.local.room.relations.JobEntityAndRelations
import com.google.android.ground.persistence.local.room.relations.SurveyEntityAndRelations
import com.google.android.ground.persistence.local.room.relations.TaskEntityAndRelations
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java8.util.Optional
import kotlinx.collections.immutable.toPersistentList
import org.json.JSONObject
import timber.log.Timber

fun AuditInfo.toLocalDataStoreObject(): AuditInfoEntity =
  AuditInfoEntity(
    user = UserDetails.fromUser(user),
    clientTimestamp = clientTimestamp.time,
    serverTimestamp = serverTimestamp.map { obj: Date -> obj.time }.orElse(null)
  )

fun AuditInfoEntity.toModelObject() =
  AuditInfo(
    UserDetails.toUser(user),
    Date(clientTimestamp),
    Optional.ofNullable(serverTimestamp).map { Date(it!!) }
  )

private fun BaseMap.BaseMapType.toLocalDataStoreObject() =
  when (this) {
    BaseMap.BaseMapType.TILED_WEB_MAP -> BaseMapEntity.BaseMapEntityType.IMAGE
    BaseMap.BaseMapType.MBTILES_FOOTPRINTS -> BaseMapEntity.BaseMapEntityType.GEOJSON
    else -> BaseMapEntity.BaseMapEntityType.UNKNOWN
  }

private fun BaseMapEntity.BaseMapEntityType.toModelObject() =
  when (this) {
    BaseMapEntity.BaseMapEntityType.IMAGE -> BaseMap.BaseMapType.TILED_WEB_MAP
    BaseMapEntity.BaseMapEntityType.GEOJSON -> BaseMap.BaseMapType.MBTILES_FOOTPRINTS
    else -> BaseMap.BaseMapType.UNKNOWN
  }

fun BaseMap.toLocalDataStoreObject(surveyId: String) =
  BaseMapEntity(surveyId = surveyId, url = url.toString(), type = type.toLocalDataStoreObject())

fun BaseMapEntity.toModelObject() = BaseMap(url = URL(url), type = type.toModelObject())

fun Geometry.toLocalDataStoreObject() = GeometryWrapper.fromGeometry(this)

fun formatVertices(vertices: List<Point>): String? {
  if (vertices.isEmpty()) {
    return null
  }
  val gson = Gson()
  val verticesArray =
    vertices.map { (coordinate): Point -> ImmutableList.of(coordinate.x, coordinate.y) }.toList()
  return gson.toJson(verticesArray)
}

fun parseVertices(vertices: String?): ImmutableList<Point> {
  if (vertices.isNullOrEmpty()) {
    return ImmutableList.of()
  }
  val gson = Gson()
  val verticesArray =
    gson.fromJson<List<List<Double>>>(vertices, object : TypeToken<List<List<Double?>?>?>() {}.type)
  return verticesArray
    .map { vertex: List<Double> -> Point(Coordinate(vertex[0], vertex[1])) }
    .toImmutableList()
}

fun Job.toLocalDataStoreObject(surveyId: String) =
  JobEntity(id = id, surveyId = surveyId, name = name)

fun JobEntityAndRelations.toModelObject(): Job {
  val taskMap = ImmutableMap.builder<String, Task>()

  for (taskEntityAndRelations in taskEntityAndRelations) {
    val task = taskEntityAndRelations.toModelObject()
    taskMap.put(task.id, task)
  }

  return Job(jobEntity.id, jobEntity.name, taskMap.build())
}

fun LocationOfInterest.toLocalDataStoreObject() =
  LocationOfInterestEntity(
    id = id,
    surveyId = surveyId,
    jobId = job.id,
    state = EntityState.DEFAULT,
    created = created.toLocalDataStoreObject(),
    lastModified = lastModified.toLocalDataStoreObject(),
    geometry = geometry.toLocalDataStoreObject()
  )

fun LocationOfInterestEntity.toModelObject(survey: Survey): LocationOfInterest {
  if (geometry == null) {
    throw LocalDataConsistencyException("No geometry in location of interest $this.id")
  } else {
    return LocationOfInterest(
      id = id,
      surveyId = surveyId,
      created = created.toModelObject(),
      lastModified = lastModified.toModelObject(),
      geometry = geometry.getGeometry(),
      job =
        survey.getJob(jobId = jobId).orElseThrow {
          LocalDataConsistencyException(
            "Unknown jobId $this.jobId in location of interest $this.id"
          )
        }
    )
  }
}

fun LocationOfInterestMutation.toLocalDataStoreObject(
  created: AuditInfo
): LocationOfInterestEntity {
  val authInfo = created.toLocalDataStoreObject()

  return LocationOfInterestEntity(
    id = locationOfInterestId,
    surveyId = surveyId,
    jobId = jobId,
    state = EntityState.DEFAULT,
    created = authInfo,
    lastModified = authInfo,
    geometry = geometry
  )
}

fun LocationOfInterestMutation.toLocalDataStoreObject() =
  LocationOfInterestMutationEntity(
    id = id,
    surveyId = surveyId,
    jobId = jobId,
    type = MutationEntityType.fromMutationType(type),
    newGeometry = geometry,
    userId = userId,
    locationOfInterestId = locationOfInterestId,
    syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(syncStatus),
    clientTimestamp = clientTimestamp.time,
    lastError = lastError,
    retryCount = retryCount
  )

fun LocationOfInterestMutationEntity.toModelObject() =
  LocationOfInterestMutation(
    id = id,
    surveyId = surveyId,
    jobId = jobId,
    type = type.toMutationType(),
    geometry = newGeometry,
    userId = userId,
    locationOfInterestId = locationOfInterestId,
    syncStatus = syncStatus.toMutationSyncStatus(),
    clientTimestamp = Date(clientTimestamp),
    lastError = lastError,
    retryCount = retryCount,
  )

fun MultipleChoiceEntity.toModelObject(optionEntities: List<OptionEntity>): MultipleChoice {
  val listBuilder = ImmutableList.builder<Option>()

  for (optionEntity in optionEntities) {
    listBuilder.add(optionEntity.toModelObject())
  }

  return MultipleChoice(listBuilder.build().toPersistentList(), this.type.toCardinality())
}

fun MultipleChoice.toLocalDataStoreObject(taskId: String): MultipleChoiceEntity =
  MultipleChoiceEntity(taskId, MultipleChoiceEntityType.fromCardinality(this.cardinality))

private fun OfflineAreaEntityState.toModelObject() =
  when (this) {
    OfflineAreaEntityState.PENDING -> OfflineArea.State.PENDING
    OfflineAreaEntityState.IN_PROGRESS -> OfflineArea.State.IN_PROGRESS
    OfflineAreaEntityState.DOWNLOADED -> OfflineArea.State.DOWNLOADED
    OfflineAreaEntityState.FAILED -> OfflineArea.State.FAILED
    else -> throw IllegalArgumentException("Unknown area state: $this")
  }

private fun OfflineArea.State.toLocalDataStoreObject() =
  when (this) {
    OfflineArea.State.PENDING -> OfflineAreaEntityState.PENDING
    OfflineArea.State.IN_PROGRESS -> OfflineAreaEntityState.IN_PROGRESS
    OfflineArea.State.FAILED -> OfflineAreaEntityState.FAILED
    OfflineArea.State.DOWNLOADED -> OfflineAreaEntityState.DOWNLOADED
  }

fun OfflineArea.toOfflineAreaEntity() =
  OfflineAreaEntity(
    id = this.id,
    state = this.state.toLocalDataStoreObject(),
    name = this.name,
    north = this.bounds.northeast.latitude,
    east = this.bounds.northeast.longitude,
    south = this.bounds.southwest.latitude,
    west = this.bounds.southwest.longitude
  )

fun OfflineAreaEntity.toModelObject(): OfflineArea {
  val northEast = LatLng(this.north, this.east)
  val southWest = LatLng(this.south, this.west)
  val bounds = LatLngBounds(southWest, northEast)

  return OfflineArea(this.id, this.state.toModelObject(), bounds, this.name)
}

fun Option.toLocalDataStoreObject(taskId: String) =
  OptionEntity(id = this.id, code = this.code, label = this.label, taskId = taskId)

fun OptionEntity.toModelObject() = Option(id = this.id, code = this.code, label = this.label)

fun SubmissionEntity.toModelObject(loi: LocationOfInterest): Submission {
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
    created = this.created.toModelObject(),
    lastModified = this.lastModified.toModelObject(),
    responses = ResponseMapConverter.fromString(job, this.responses)
  )
}

fun Submission.toLocalDataStoreObject() =
  SubmissionEntity(
    id = this.id,
    jobId = this.job.id,
    locationOfInterestId = this.locationOfInterest.id,
    state = EntityState.DEFAULT,
    responses = ResponseMapConverter.toString(this.responses),
    created = this.created.toLocalDataStoreObject(),
    lastModified = this.lastModified.toLocalDataStoreObject(),
  )

fun SubmissionMutation.toLocalDataStoreObject(created: AuditInfo): SubmissionEntity {
  val authInfo = created.toLocalDataStoreObject()

  return SubmissionEntity(
    id = this.submissionId,
    jobId = this.job!!.id,
    locationOfInterestId = this.locationOfInterestId,
    state = EntityState.DEFAULT,
    responses = ResponseMapConverter.toString(TaskDataMap().copyWithDeltas(this.taskDataDeltas)),
    created = authInfo,
    lastModified = authInfo
  )
}

@Throws(LocalDataConsistencyException::class)
fun SubmissionMutationEntity.toModelObject(survey: Survey): SubmissionMutation {
  val job =
    survey.getJob(jobId).orElseThrow {
      LocalDataConsistencyException("Unknown jobId in submission mutation $id")
    }

  return SubmissionMutation(
    job = job,
    submissionId = submissionId,
    taskDataDeltas = ResponseDeltasConverter.fromString(job, responseDeltas),
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

fun SubmissionMutation.toLocalDataStoreObject() =
  SubmissionMutationEntity(
    id = id,
    surveyId = surveyId,
    locationOfInterestId = locationOfInterestId,
    jobId = job!!.id,
    submissionId = submissionId,
    type = MutationEntityType.fromMutationType(type),
    syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(syncStatus),
    responseDeltas = ResponseDeltasConverter.toString(taskDataDeltas),
    retryCount = retryCount,
    lastError = lastError,
    userId = userId,
    clientTimestamp = clientTimestamp.time
  )

fun SurveyEntityAndRelations.toModelObject(): Survey {
  val jobMap = ImmutableMap.builder<String, Job>()
  val baseMaps = ImmutableList.builder<BaseMap>()

  for (jobEntityAndRelations in jobEntityAndRelations) {
    val job = jobEntityAndRelations.toModelObject()
    jobMap.put(job.id, job)
  }
  for (source in baseMapEntityAndRelations) {
    try {
      baseMaps.add(source.toModelObject())
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

fun Survey.toLocalDataStoreObject() =
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

    multipleChoice = multipleChoiceEntities[0].toModelObject(optionEntities)
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
