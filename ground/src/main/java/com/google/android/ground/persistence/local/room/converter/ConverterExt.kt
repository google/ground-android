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

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.job.Job.DataCollectionStrategy
import com.google.android.ground.model.job.Style
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.DraftSubmission
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.SubmissionData
import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.Expression
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.model.task.TaskId
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.entity.*
import com.google.android.ground.persistence.local.room.fields.*
import com.google.android.ground.persistence.local.room.relations.ConditionEntityAndRelations
import com.google.android.ground.persistence.local.room.relations.JobEntityAndRelations
import com.google.android.ground.persistence.local.room.relations.SurveyEntityAndRelations
import com.google.android.ground.persistence.local.room.relations.TaskEntityAndRelations
import com.google.android.ground.proto.Survey.DataSharingTerms
import com.google.android.ground.ui.map.Bounds
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.*
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import org.json.JSONObject
import timber.log.Timber

fun AuditInfo.toLocalDataStoreObject(): AuditInfoEntity =
  AuditInfoEntity(
    user = UserDetails.fromUser(user),
    clientTimestamp = clientTimestamp.time,
    serverTimestamp = serverTimestamp?.time,
  )

fun AuditInfoEntity.toModelObject() =
  AuditInfo(UserDetails.toUser(user), Date(clientTimestamp), serverTimestamp?.let { Date(it) })

fun Geometry.toLocalDataStoreObject() = GeometryWrapper.fromGeometry(this)

fun formatVertices(vertices: List<Point>): String? {
  if (vertices.isEmpty()) {
    return null
  }
  val gson = Gson()
  val verticesArray = vertices.map { (coordinate): Point -> listOf(coordinate.lat, coordinate.lng) }
  return gson.toJson(verticesArray)
}

fun parseVertices(vertices: String?): List<Point> {
  if (vertices.isNullOrEmpty()) {
    return listOf()
  }
  val gson = Gson()
  val verticesArray =
    gson.fromJson<List<List<Double>>>(vertices, object : TypeToken<List<List<Double?>?>?>() {}.type)
  return verticesArray.map { vertex: List<Double> -> Point(Coordinates(vertex[0], vertex[1])) }
}

fun Job.toLocalDataStoreObject(surveyId: String): JobEntity =
  JobEntity(
    id = id,
    surveyId = surveyId,
    name = name,
    strategy = strategy.toString(),
    style = style?.toLocalDataStoreObject(),
  )

fun JobEntityAndRelations.toModelObject(): Job {
  val taskMap = taskEntityAndRelations.map { it.toModelObject() }.associateBy { it.id }
  return Job(
    id = jobEntity.id,
    style = jobEntity.style?.toModelObject(),
    name = jobEntity.name,
    strategy =
      jobEntity.strategy.let {
        try {
          DataCollectionStrategy.valueOf(it)
        } catch (e: IllegalArgumentException) {
          Timber.e(e, "Unknown data collection strategy $it")
          DataCollectionStrategy.UNKNOWN
        }
      },
    tasks = taskMap.toPersistentMap(),
  )
}

/**
 * Returns the equivalent model object, setting the style color to #000 if it was missing in the
 * local db.
 */
fun StyleEntity.toModelObject() = color?.let { Style(it) } ?: Style("#000000")

fun Style.toLocalDataStoreObject() = StyleEntity(color)

fun LocationOfInterest.toLocalDataStoreObject() =
  LocationOfInterestEntity(
    id = id,
    surveyId = surveyId,
    jobId = job.id,
    deletionState = EntityDeletionState.DEFAULT,
    created = created.toLocalDataStoreObject(),
    lastModified = lastModified.toLocalDataStoreObject(),
    geometry = geometry.toLocalDataStoreObject(),
    customId = customId,
    submissionCount = submissionCount,
    properties = properties,
    isPredefined = isPredefined,
  )

fun LocationOfInterestEntity.toModelObject(survey: Survey): LocationOfInterest =
  if (geometry == null) {
    throw LocalDataConsistencyException("No geometry in location of interest $this.id")
  } else {
    LocationOfInterest(
      id = id,
      surveyId = surveyId,
      created = created.toModelObject(),
      lastModified = lastModified.toModelObject(),
      customId = customId,
      geometry = geometry.getGeometry(),
      submissionCount = submissionCount,
      properties = properties,
      isPredefined = isPredefined,
      job =
        survey.getJob(jobId = jobId)
          ?: throw LocalDataConsistencyException(
            "Unknown jobId ${this.jobId} in location of interest ${this.id}"
          ),
    )
  }

@Deprecated(
  "Use toLocalDataStoreObject(User) instead",
  ReplaceWith("toLocalDataStoreObject(auditInfo.user)"),
)
fun LocationOfInterestMutation.toLocalDataStoreObject(auditInfo: AuditInfo) =
  toLocalDataStoreObject(auditInfo.user)

fun LocationOfInterestMutation.toLocalDataStoreObject(user: User): LocationOfInterestEntity {
  val auditInfo = AuditInfo(user, clientTimestamp).toLocalDataStoreObject()

  return LocationOfInterestEntity(
    id = locationOfInterestId,
    surveyId = surveyId,
    jobId = jobId,
    deletionState = EntityDeletionState.DEFAULT,
    // TODO(#1562): Preserve creation audit info for UPDATE mutations.
    created = auditInfo,
    lastModified = auditInfo,
    geometry = geometry?.toLocalDataStoreObject(),
    customId = customId,
    submissionCount = submissionCount,
    properties = properties,
    isPredefined = isPredefined,
  )
}

fun LocationOfInterestMutation.toLocalDataStoreObject() =
  LocationOfInterestMutationEntity(
    id = id,
    surveyId = surveyId,
    jobId = jobId,
    type = MutationEntityType.fromMutationType(type),
    newGeometry = geometry?.toLocalDataStoreObject(),
    newCustomId = customId,
    userId = userId,
    locationOfInterestId = locationOfInterestId,
    syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(syncStatus),
    clientTimestamp = clientTimestamp.time,
    lastError = lastError,
    retryCount = retryCount,
    newProperties = properties,
    isPredefined = isPredefined,
    collectionId = collectionId,
  )

fun LocationOfInterestMutationEntity.toModelObject() =
  LocationOfInterestMutation(
    id = id,
    surveyId = surveyId,
    jobId = jobId,
    type = type.toMutationType(),
    geometry = newGeometry?.getGeometry(),
    customId = newCustomId,
    userId = userId,
    locationOfInterestId = locationOfInterestId,
    syncStatus = syncStatus.toMutationSyncStatus(),
    clientTimestamp = Date(clientTimestamp),
    lastError = lastError,
    retryCount = retryCount,
    properties = newProperties,
    isPredefined = isPredefined,
    collectionId = collectionId,
  )

fun MultipleChoiceEntity.toModelObject(optionEntities: List<OptionEntity>): MultipleChoice {
  val options = optionEntities.map { it.toModelObject() }
  return MultipleChoice(options.toPersistentList(), this.type.toCardinality(), this.hasOtherOption)
}

fun MultipleChoice.toLocalDataStoreObject(taskId: String): MultipleChoiceEntity =
  MultipleChoiceEntity(
    taskId,
    MultipleChoiceEntityType.fromCardinality(this.cardinality),
    hasOtherOption,
  )

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
    north = this.bounds.north,
    east = this.bounds.east,
    south = this.bounds.south,
    west = this.bounds.west,
    minZoom = this.zoomRange.first,
    maxZoom = this.zoomRange.last,
  )

fun OfflineAreaEntity.toModelObject(): OfflineArea {
  val northEast = Coordinates(this.north, this.east)
  val southWest = Coordinates(this.south, this.west)
  val bounds = Bounds(southWest, northEast)
  return OfflineArea(
    this.id,
    this.state.toModelObject(),
    bounds,
    this.name,
    IntRange(minZoom, maxZoom),
  )
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
    data = SubmissionDataConverter.fromString(job, this.data),
  )
}

fun Submission.toLocalDataStoreObject() =
  SubmissionEntity(
    id = this.id,
    jobId = this.job.id,
    locationOfInterestId = this.locationOfInterest.id,
    deletionState = EntityDeletionState.DEFAULT,
    data = SubmissionDataConverter.toString(this.data),
    created = this.created.toLocalDataStoreObject(),
    lastModified = this.lastModified.toLocalDataStoreObject(),
  )

fun SubmissionMutation.toLocalDataStoreObject(created: AuditInfo): SubmissionEntity {
  val auditInfo = created.toLocalDataStoreObject()

  return SubmissionEntity(
    id = this.submissionId,
    jobId = this.job.id,
    locationOfInterestId = this.locationOfInterestId,
    deletionState = EntityDeletionState.DEFAULT,
    data = SubmissionDataConverter.toString(SubmissionData().copyWithDeltas(this.deltas)),
    // TODO(#1562): Preserve creation audit info for UPDATE mutations.
    created = auditInfo,
    lastModified = auditInfo,
  )
}

@Throws(LocalDataConsistencyException::class)
fun SubmissionMutationEntity.toModelObject(survey: Survey): SubmissionMutation {
  val job =
    survey.getJob(jobId)
      ?: throw LocalDataConsistencyException("Unknown jobId $jobId in submission mutation $id")

  return SubmissionMutation(
    job = job,
    submissionId = submissionId,
    deltas = SubmissionDeltasConverter.fromString(job, deltas),
    id = id,
    surveyId = surveyId,
    locationOfInterestId = locationOfInterestId,
    type = type.toMutationType(),
    syncStatus = syncStatus.toMutationSyncStatus(),
    retryCount = retryCount,
    lastError = lastError,
    userId = userId,
    clientTimestamp = Date(clientTimestamp),
    collectionId = collectionId,
  )
}

fun SubmissionMutation.toLocalDataStoreObject() =
  SubmissionMutationEntity(
    id = id,
    surveyId = surveyId,
    locationOfInterestId = locationOfInterestId,
    jobId = job.id,
    submissionId = submissionId,
    type = MutationEntityType.fromMutationType(type),
    syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(syncStatus),
    deltas = SubmissionDeltasConverter.toString(deltas),
    retryCount = retryCount,
    lastError = lastError,
    userId = userId,
    clientTimestamp = clientTimestamp.time,
    collectionId = collectionId,
  )

fun SurveyEntityAndRelations.toModelObject(): Survey {
  val jobMap = jobEntityAndRelations.map { it.toModelObject() }.associateBy { it.id }

  return Survey(
    surveyEntity.id,
    surveyEntity.title!!,
    surveyEntity.description!!,
    jobMap.toPersistentMap(),
    surveyEntity.acl?.toStringMap()!!,
    surveyEntity.dataSharingTerms?.let { DataSharingTerms.parseFrom(surveyEntity.dataSharingTerms) },
  )
}

private fun JSONObject.toStringMap(): Map<String, String> {
  val builder = mutableMapOf<String, String>()
  keys().forEach { key: String -> builder[key] = optString(key, toString()) }
  return builder.toPersistentMap()
}

fun Survey.toLocalDataStoreObject() =
  SurveyEntity(
    id = id,
    title = title,
    description = description,
    acl = JSONObject(acl as Map<*, *>),
    dataSharingTerms = dataSharingTerms?.toByteArray(),
  )

fun Task.toLocalDataStoreObject(jobId: String?) =
  TaskEntity(
    id = id,
    jobId = jobId,
    index = index,
    label = label,
    isRequired = isRequired,
    taskType = TaskEntityType.fromTaskType(type),
    isAddLoiTask = isAddLoiTask,
  )

fun TaskEntityAndRelations.toModelObject(): Task {
  var multipleChoice: MultipleChoice? = null

  if (multipleChoiceEntities.isNotEmpty()) {
    if (multipleChoiceEntities.size > 1) {
      Timber.e("More than 1 multiple choice found for task: $taskEntity")
    }

    multipleChoice = multipleChoiceEntities[0].toModelObject(optionEntities)
  }

  var condition: Condition? = null

  if (conditionEntityAndRelations.isNotEmpty()) {
    if (conditionEntityAndRelations.size > 1) {
      Timber.e("More than 1 condition found for task: $taskEntity")
    }
    condition = conditionEntityAndRelations[0].toModelObject()
  }

  return Task(
    taskEntity.id,
    taskEntity.index,
    taskEntity.taskType.toTaskType(),
    taskEntity.label!!,
    taskEntity.isRequired,
    multipleChoice,
    taskEntity.isAddLoiTask,
    condition,
  )
}

fun User.toLocalDataStoreObject() =
  UserEntity(id = id, email = email, displayName = displayName, photoUrl = photoUrl)

fun UserEntity.toModelObject() =
  User(id = id, email = email, displayName = displayName, photoUrl = photoUrl)

fun Condition.toLocalDataStoreObject(parentTaskId: TaskId) =
  ConditionEntity(parentTaskId = parentTaskId, matchType = MatchEntityType.fromMatchType(matchType))

fun ConditionEntityAndRelations.toModelObject(): Condition? {
  val expressions: List<Expression>?

  if (expressionEntities.isEmpty()) {
    return null
  } else {
    expressions = expressionEntities.map { it.toModelObject() }
  }

  return Condition(conditionEntity.matchType.toMatchType(), expressions = expressions)
}

fun Expression.toLocalDataStoreObject(parentTaskId: TaskId): ExpressionEntity =
  ExpressionEntity(
    parentTaskId = parentTaskId,
    expressionType = ExpressionEntityType.fromExpressionType(expressionType),
    taskId = taskId,
    optionIds = optionIds.joinToString(","),
  )

fun ExpressionEntity.toModelObject(): Expression =
  Expression(
    expressionType = expressionType.toExpressionType(),
    taskId = taskId,
    optionIds = optionIds?.split(',')?.toSet() ?: setOf(),
  )

@Throws(LocalDataConsistencyException::class)
fun DraftSubmissionEntity.toModelObject(survey: Survey): DraftSubmission {
  val job =
    survey.getJob(jobId)
      ?: throw LocalDataConsistencyException("Unknown jobId in submission mutation $id")

  return DraftSubmission(
    id = id,
    jobId = jobId,
    loiId = loiId,
    loiName = loiName,
    surveyId = surveyId,
    deltas = SubmissionDeltasConverter.fromString(job, deltas),
  )
}

fun DraftSubmission.toLocalDataStoreObject() =
  DraftSubmissionEntity(
    id = id,
    jobId = jobId,
    loiId = loiId,
    loiName = loiName,
    surveyId = surveyId,
    deltas = SubmissionDeltasConverter.toString(deltas),
  )
