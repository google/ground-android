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

import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.submission.TaskDataMap
import com.google.android.ground.persistence.local.room.LocalDataStoreException
import com.google.android.ground.persistence.local.room.converter.ResponseDeltasConverter
import com.google.android.ground.persistence.local.room.converter.ResponseMapConverter
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.SubmissionDao
import com.google.android.ground.persistence.local.room.dao.SubmissionMutationDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdateSuspend
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.fields.EntityState
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.fields.UserDetails
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.util.Debug.logOnFailure
import com.google.common.base.Preconditions
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx2.rxCompletable
import timber.log.Timber

/** Manages access to [Submission] objects persisted in local storage. */
@Singleton
class RoomSubmissionStore @Inject internal constructor() : LocalSubmissionStore {
  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
  @Inject lateinit var submissionDao: SubmissionDao
  @Inject lateinit var submissionMutationDao: SubmissionMutationDao
  @Inject lateinit var userStore: RoomUserStore
  @Inject lateinit var schedulers: Schedulers

  /**
   * Attempts to retrieve the [Submission] associated with the given ID and [LocationOfInterest].
   *
   * @throws LocalDataStoreException
   */
  override suspend fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String
  ): Submission =
    submissionDao.findByIdSuspend(submissionId)?.toModelObject(locationOfInterest)
      ?: throw LocalDataStoreException("Submission not found $submissionId")

  /**
   * Attempts to retrieve the complete list of [Submission]s associated with the given Job ID and
   * [LocationOfInterest]. Returns a [Single] that contains an exception if no such Submission exist
   * or the list of submissions otherwise. Does not stream subsequent data changes.
   */
  override suspend fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String
  ): List<Submission> =
    submissionDao
      .findByLocationOfInterestId(locationOfInterest.id, jobId, EntityState.DEFAULT)
      ?.mapNotNull { logOnFailure { it.toModelObject(locationOfInterest) } }
      ?: listOf()

  override suspend fun merge(model: Submission) {
    submissionMutationDao
      .findBySubmissionId(
        model.id,
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS
      )
      ?.let { mergeSubmission(model.job, model.toLocalDataStoreObject(), it) }
  }

  override fun enqueue(mutation: SubmissionMutation): Completable =
    submissionMutationDao
      .insert(mutation.toLocalDataStoreObject())
      .doOnSubscribe { Timber.v("Enqueuing mutation: $mutation") }
      .subscribeOn(schedulers.io())

  /**
   * Applies mutation to submission in database or creates a new one.
   *
   * @return A Completable that emits an error if mutation type is "UPDATE" but entity does not
   * exist, or if type is "CREATE" and entity already exists.
   */
  override fun apply(mutation: SubmissionMutation): Completable =
    rxCompletable(ioDispatcher) {
      when (mutation.type) {
        Mutation.Type.CREATE -> {
          val user = userStore.getUser(mutation.userId)
          val entity = mutation.toLocalDataStoreObject(AuditInfo(user))
          submissionDao.insertOrUpdateSuspend(entity)
        }
        Mutation.Type.UPDATE -> {
          val user = userStore.getUser(mutation.userId)
          updateSubmission(mutation, user)
        }
        Mutation.Type.DELETE -> {
          val entity = checkNotNull(submissionDao.findByIdSuspend(mutation.submissionId))
          submissionDao.updateSuspend(entity.copy(state = EntityState.DELETED))
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
    val entity =
      submissionDao.findByIdSuspend(mutation.submissionId) ?: fallbackSubmission(mutation)
    commitMutations(mutation.job, entity, listOf(mutationEntity), user)
    submissionDao.insertOrUpdateSuspend(entity)
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
    mutations: List<SubmissionMutationEntity>
  ) {
    if (mutations.isEmpty()) {
      return submissionDao.insertOrUpdateSuspend(submission)
    }
    val lastMutation = mutations[mutations.size - 1]
    Preconditions.checkNotNull(lastMutation, "Could not get last mutation")
    return userStore
      .getUser(lastMutation.userId)
      .let { commitMutations(job, submission, mutations, it) }
      .let { submissionDao.insertOrUpdateSuspend(it) }
  }

  private fun commitMutations(
    job: Job?,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>,
    user: User
  ): SubmissionEntity {
    val lastMutation = mutations[mutations.size - 1]
    val clientTimestamp = lastMutation.clientTimestamp

    Timber.v("Merging submission $this with mutations $mutations")

    return submission.copy(
      responses = ResponseMapConverter.toString(commitMutations(job, submission, mutations)),
      lastModified = AuditInfoEntity(UserDetails.fromUser(user), clientTimestamp)
    )
  }

  private fun commitMutations(
    job: Job?,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>
  ): TaskDataMap {
    val responseMap = ResponseMapConverter.fromString(job!!, submission.responses)
    val deltas = mutableListOf<TaskDataDelta>()
    for (mutation in mutations) {
      // Merge changes to responses.
      deltas.addAll(ResponseDeltasConverter.fromString(job, mutation.responseDeltas))
    }
    return responseMap.copyWithDeltas(deltas.toPersistentList())
  }

  override suspend fun deleteSubmission(submissionId: String) {
    submissionDao.findByIdSuspend(submissionId)?.let { submissionDao.deleteSuspend(it) }
  }

  override fun getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<List<SubmissionMutation>> =
    submissionMutationDao
      .findByLocationOfInterestIdOnceAndStream(locationOfInterestId, *allowedStates)
      .map { list: List<SubmissionMutationEntity> -> list.map { it.toModelObject(survey) } }

  override fun applyAndEnqueue(mutation: SubmissionMutation): Completable =
    try {
      apply(mutation).andThen(enqueue(mutation))
    } catch (e: LocalDataStoreException) {
      FirebaseCrashlytics.getInstance()
        .log("Error enqueueing ${mutation.type} mutation for submission ${mutation.submissionId}")
      FirebaseCrashlytics.getInstance().recordException(e)
      Completable.error(e)
    }

  override fun getAllMutationsAndStream(): Flowable<List<SubmissionMutationEntity>> =
    submissionMutationDao.loadAllOnceAndStream()

  override suspend fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus
  ): List<SubmissionMutationEntity> = submissionMutationDao.findByLocationOfInterestId(id, *states)
}
