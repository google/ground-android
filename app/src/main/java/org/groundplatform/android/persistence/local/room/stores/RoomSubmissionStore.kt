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
package org.groundplatform.android.persistence.local.room.stores

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.groundplatform.android.model.AuditInfo
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.User
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.model.submission.DraftSubmission
import org.groundplatform.android.model.submission.Submission
import org.groundplatform.android.model.submission.SubmissionData
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.persistence.local.room.LocalDataStoreException
import org.groundplatform.android.persistence.local.room.converter.SubmissionDataConverter
import org.groundplatform.android.persistence.local.room.converter.SubmissionDeltasConverter
import org.groundplatform.android.persistence.local.room.converter.toLocalDataStoreObject
import org.groundplatform.android.persistence.local.room.converter.toModelObject
import org.groundplatform.android.persistence.local.room.dao.DraftSubmissionDao
import org.groundplatform.android.persistence.local.room.dao.SubmissionDao
import org.groundplatform.android.persistence.local.room.dao.SubmissionMutationDao
import org.groundplatform.android.persistence.local.room.dao.insertOrUpdate
import org.groundplatform.android.persistence.local.room.entity.AuditInfoEntity
import org.groundplatform.android.persistence.local.room.entity.SubmissionEntity
import org.groundplatform.android.persistence.local.room.entity.SubmissionMutationEntity
import org.groundplatform.android.persistence.local.room.fields.EntityDeletionState
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.persistence.local.room.fields.MutationEntityType
import org.groundplatform.android.persistence.local.room.fields.UserDetails
import org.groundplatform.android.persistence.local.stores.LocalSubmissionStore
import org.groundplatform.android.util.Debug.logOnFailure
import timber.log.Timber

/** Manages access to [Submission] objects persisted in local storage. */
@Singleton
class RoomSubmissionStore @Inject internal constructor() : LocalSubmissionStore {
  @Inject lateinit var draftSubmissionDao: DraftSubmissionDao
  @Inject lateinit var submissionDao: SubmissionDao
  @Inject lateinit var submissionMutationDao: SubmissionMutationDao
  @Inject lateinit var userStore: RoomUserStore
  @Inject lateinit var surveyStore: RoomSurveyStore

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
      .findByLocationOfInterestId(locationOfInterest.id, jobId, EntityDeletionState.DEFAULT)
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
        submissionDao.update(entity.copy(deletionState = EntityDeletionState.DELETED))
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
      Timber.e(
        e,
        "Error enqueueing ${mutation.type} mutation for submission ${mutation.submissionId}",
      )
      throw e
    }
  }

  override fun getAllSurveyMutationsFlow(survey: Survey): Flow<List<SubmissionMutation>> =
    submissionMutationDao
      .getAllMutationsFlow()
      .map { mutations ->
        mutations.filter { it.surveyId == survey.id }.map { it.toModelObject(survey) }
      }
      .catch { Timber.e(it, "Ignoring invalid submission mutation") }

  override fun getAllMutationsFlow(): Flow<List<SubmissionMutation>> =
    submissionMutationDao
      .getAllMutationsFlow()
      .map { it.mapNotNull { mutation -> convertMutation(mutation) } }
      .catch { Timber.e(it, "Ignoring invalid submission mutation") }

  private suspend fun convertMutation(mutation: SubmissionMutationEntity): SubmissionMutation? {
    val survey = surveyStore.getSurveyById(mutation.surveyId)
    return survey?.let { mutation.toModelObject(survey) }
  }

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

  override suspend fun countDraftSubmissions(): Int = draftSubmissionDao.countAll()
}
