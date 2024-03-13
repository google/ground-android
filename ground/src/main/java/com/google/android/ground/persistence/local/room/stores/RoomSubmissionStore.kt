/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.persistence.local.room.stores

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.DraftSubmission
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.SubmissionData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.persistence.local.room.LocalDataStoreException
import com.google.android.ground.persistence.local.room.converter.SubmissionDataConverter
import com.google.android.ground.persistence.local.room.converter.SubmissionDeltasConverter
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.DraftSubmissionDao
import com.google.android.ground.persistence.local.room.dao.SubmissionDao
import com.google.android.ground.persistence.local.room.dao.SubmissionMutationDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.fields.EntityState
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.fields.MutationEntityType
import com.google.android.ground.persistence.local.room.fields.UserDetails
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.util.Debug.logOnFailure
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

/** Manages access to [Submission] objects persisted in local storage. */
@Singleton
class RoomSubmissionStore @Inject internal constructor() : LocalSubmissionStore {
  @Inject lateinit var draftSubmissionDao: DraftSubmissionDao
  @Inject lateinit var submissionDao: SubmissionDao
  @Inject lateinit var submissionMutationDao: SubmissionMutationDao
  @Inject lateinit var userStore: RoomUserStore

  /**
   * Attempts to retrieve the [Submission] associated with the given ID and [LocationOfInterest].
   *
   * @throws LocalDataStoreException
   */
  override suspend fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String,
  ): Submission =
    submissionDao.findById(submissionId)?.toModelObject(locationOfInterest)
      ?: throw LocalDataStoreException("Submission not found $submissionId")

  /**
   * Attempts to retrieve the complete list of [Submission]s associated with the given Job ID and
   * [LocationOfInterest]. Returns an empty list if no such submissions exist or the list of
   * submissions otherwise.
   */
  override suspend fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String,
  ): List<Submission> =
    submissionDao
      .findByLocationOfInterestId(locationOfInterest.id, jobId, EntityState.DEFAULT)
      ?.mapNotNull { logOnFailure { it.toModelObject(locationOfInterest) } } ?: listOf()

  override suspend fun merge(model: Submission) {
    submissionMutationDao
      .findBySubmissionId(
        model.id,
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS,
      )
      ?.let { mergeSubmission(model.job, model.toLocalDataStoreObject(), it) }
  }

  override suspend fun enqueue(mutation: SubmissionMutation) =
    submissionMutationDao.insert(mutation.toLocalDataStoreObject())

  /**
   * Applies mutation to submission in database or creates a new one.
   *
   * @return A Completable that emits an error if mutation type is "UPDATE" but entity does not
   *   exist, or if type is "CREATE" and entity already exists.
   */
  override suspend fun apply(mutation: SubmissionMutation) {
    when (mutation.type) {
      Mutation.Type.CREATE -> {
        val user = userStore.getUser(mutation.userId)
        val entity = mutation.toLocalDataStoreObject(AuditInfo(user))
        submissionDao.insertOrUpdate(entity)
      }
      Mutation.Type.UPDATE -> {
        val user = userStore.getUser(mutation.userId)
        updateSubmission(mutation, user)
      }
      Mutation.Type.DELETE -> {
        val entity = checkNotNull(submissionDao.findById(mutation.submissionId))
        submissionDao.update(entity.copy(state = EntityState.DELETED))
      }
      Mutation.Type.UNKNOWN -> {
        throw LocalDataStoreException("Unknown Mutation.Type")
      }
    }
  }

  override suspend fun updateAll(mutations: List<SubmissionMutation>) {
    submissionMutationDao.updateAll(mutations.map { it.toLocalDataStoreObject() })
  }

  private suspend fun updateSubmission(mutation: SubmissionMutation, user: User) {
    Timber.v("Applying mutation: $mutation")
    val mutationEntity = mutation.toLocalDataStoreObject()
    val entity = submissionDao.findById(mutation.submissionId) ?: fallbackSubmission(mutation)
    commitMutations(mutation.job, entity, listOf(mutationEntity), user)
    submissionDao.insertOrUpdate(entity)
  }

  /**
   * Returns a source which creates an submission based on the provided mutation. Used in rare cases
   * when the submission is no longer in the local db, but the user is updating rather than creating
   * a new submission. In these cases creation metadata is unknown, so empty audit info is used.
   */
  private fun fallbackSubmission(mutation: SubmissionMutation): SubmissionEntity =
    mutation.toLocalDataStoreObject(AuditInfo(User("", "", "")))

  private suspend fun mergeSubmission(
    job: Job,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>,
  ) {
    if (mutations.isEmpty()) {
      submissionDao.insertOrUpdate(submission)
    } else {
      val user = userStore.getUser(mutations.last().userId)
      val entity = commitMutations(job, submission, mutations, user)
      submissionDao.insertOrUpdate(entity)
    }
  }

  private fun commitMutations(
    job: Job?,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>,
    user: User,
  ): SubmissionEntity {
    val lastMutation = mutations[mutations.size - 1]
    val clientTimestamp = lastMutation.clientTimestamp

    Timber.v("Merging submission $this with mutations $mutations")

    return submission.copy(
      data = SubmissionDataConverter.toString(commitMutations(job, submission, mutations)),
      lastModified = AuditInfoEntity(UserDetails.fromUser(user), clientTimestamp),
    )
  }

  private fun commitMutations(
    job: Job?,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>,
  ): SubmissionData {
    val responseMap = SubmissionDataConverter.fromString(job!!, submission.data)
    val deltas = mutableListOf<ValueDelta>()
    for (mutation in mutations) {
      // Merge changes to submission data.
      deltas.addAll(SubmissionDeltasConverter.fromString(job, mutation.deltas))
    }
    return responseMap.copyWithDeltas(deltas.toPersistentList())
  }

  override suspend fun deleteSubmission(submissionId: String) {
    submissionDao.findById(submissionId)?.let { submissionDao.delete(it) }
  }

  override fun getSubmissionMutationsByLoiIdFlow(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus,
  ): Flow<List<SubmissionMutation>> =
    submissionMutationDao.findByLoiIdFlow(locationOfInterestId, *allowedStates).map {
      list: List<SubmissionMutationEntity> ->
      list.map { it.toModelObject(survey) }
    }

  override suspend fun applyAndEnqueue(mutation: SubmissionMutation) {
    try {
      apply(mutation)
      enqueue(mutation)
    } catch (e: LocalDataStoreException) {
      FirebaseCrashlytics.getInstance()
        .log("Error enqueueing ${mutation.type} mutation for submission ${mutation.submissionId}")
      FirebaseCrashlytics.getInstance().recordException(e)
      throw e
    }
  }

  override fun getAllSurveyMutationsFlow(survey: Survey): Flow<List<SubmissionMutation>> =
    submissionMutationDao
      .getAllMutationsFlow()
      .map { mutations ->
        mutations.filter { it.surveyId == survey.id }.map { it.toModelObject(survey) }
      }
      .catch { Timber.e("ignoring invalid submission mutation:\n\t${it.message}") }

  override suspend fun findByLocationOfInterestId(
    loidId: String,
    vararg states: MutationEntitySyncStatus,
  ): List<SubmissionMutationEntity> =
    submissionMutationDao.findByLocationOfInterestId(loidId, *states)

  override suspend fun getPendingCreateCount(loiId: String): Int =
    submissionMutationDao.getSubmissionMutationCount(
      loiId,
      MutationEntityType.CREATE,
      MutationEntitySyncStatus.PENDING,
    )

  override suspend fun getPendingDeleteCount(loiId: String): Int =
    submissionMutationDao.getSubmissionMutationCount(
      loiId,
      MutationEntityType.DELETE,
      MutationEntitySyncStatus.PENDING,
    )

  override suspend fun getDraftSubmission(
    draftSubmissionId: String,
    survey: Survey,
  ): DraftSubmission? = draftSubmissionDao.findById(draftSubmissionId)?.toModelObject(survey)

  override suspend fun saveDraftSubmission(draftSubmission: DraftSubmission) {
    draftSubmissionDao.insertOrUpdate(draftSubmission.toLocalDataStoreObject())
  }

  override suspend fun deleteDraftSubmissions() {
    draftSubmissionDao.delete()
  }
}
