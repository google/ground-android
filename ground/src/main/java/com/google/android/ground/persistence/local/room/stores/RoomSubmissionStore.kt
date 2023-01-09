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
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.models.EntityState
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.models.UserDetails
import com.google.android.ground.persistence.local.stores.SubmissionStore
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.util.toImmutableList
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.*
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Manages access to [Submission] objects persisted in local storage. */
@Singleton
class RoomSubmissionStore @Inject internal constructor() : SubmissionStore {
  @Inject lateinit var submissionDao: SubmissionDao
  @Inject lateinit var submissionMutationDao: SubmissionMutationDao
  @Inject lateinit var userStore: RoomUserStore
  @Inject lateinit var schedulers: Schedulers

  /**
   * Attempts to retrieve the [Submission] associated with the given ID and [LocationOfInterest].
   * Returns a [Maybe] that completes immediately (with no data) if the location of interest isn't
   * found and that succeeds with the location of interest otherwise (and then completes). Does not
   * stream subsequent data changes.
   */
  override fun getSubmission(
    locationOfInterest: LocationOfInterest,
    submissionId: String
  ): Maybe<Submission> =
    submissionDao
      .findById(submissionId)
      .map { it.toModelObject(locationOfInterest) }
      .doOnError { Timber.d(it) }
      .onErrorComplete()
      .subscribeOn(schedulers.io())

  /**
   * Attempts to retrieve the complete list of [Submission]s associated with the given Job ID and
   * [LocationOfInterest]. Returns a [Single] that contains an exception if no such Submission exist
   * or the list of submissions otherwise. Does not stream subsequent data changes.
   */
  override fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    jobId: String
  ): Single<ImmutableList<Submission>> =
    submissionDao
      .findByLocationOfInterestId(locationOfInterest.id, jobId, EntityState.DEFAULT)
      .map { toSubmissions(locationOfInterest, it) }
      .subscribeOn(schedulers.io())

  fun insertOrUpdate(submission: Submission): Completable =
    submissionDao.insertOrUpdate(submission.toLocalDataStoreObject())

  fun insertOrUpdate(submission: SubmissionEntity): Completable =
    submissionDao.insertOrUpdate(submission)

  override fun merge(model: Submission): Completable {
    val submissionEntity = model.toLocalDataStoreObject()
    return submissionMutationDao
      .findBySubmissionId(
        model.id,
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS
      )
      .flatMapCompletable { mergeSubmission(model.job, submissionEntity, it) }
      .subscribeOn(schedulers.io())
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
  override fun apply(mutation: SubmissionMutation): Completable {
    return when (mutation.type) {
      Mutation.Type.CREATE ->
        userStore.getUser(mutation.userId).flatMapCompletable { user ->
          insertFromMutation(mutation, user)
        }
      Mutation.Type.UPDATE ->
        userStore.getUser(mutation.userId).flatMapCompletable { user ->
          updateSubmission(mutation, user)
        }
      Mutation.Type.DELETE ->
        submissionDao.findById(mutation.submissionId).flatMapCompletable { entity ->
          markSubmissionForDeletion(entity, mutation)
        }
      Mutation.Type.UNKNOWN -> throw LocalDataStoreException("Unknown Mutation.Type")
    }
  }

  override fun updateAll(mutations: ImmutableList<SubmissionMutation>): Completable =
    submissionMutationDao.updateAll(mutations.map { it.toLocalDataStoreObject() })

  private fun markSubmissionForDeletion(
    entity: SubmissionEntity,
    mutation: SubmissionMutation
  ): Completable =
    submissionDao
      .update(entity.apply { state = EntityState.DELETED })
      .doOnSubscribe { Timber.d("Marking submission as deleted : $mutation") }
      .ignoreElement()
      .subscribeOn(schedulers.io())

  private fun updateSubmission(mutation: SubmissionMutation, user: User): Completable {
    val mutationEntity = mutation.toLocalDataStoreObject()

    return submissionDao
      .findById(mutation.submissionId)
      .doOnSubscribe { Timber.v("Applying mutation: $mutation") }
      .switchIfEmpty(fallbackSubmission(mutation))
      .map { commitMutations(mutation.job, it, ImmutableList.of(mutationEntity), user) }
      .flatMapCompletable { submissionDao.insertOrUpdate(it).subscribeOn(schedulers.io()) }
      .subscribeOn(schedulers.io())
  }

  /**
   * Returns a source which creates an submission based on the provided mutation. Used in rare cases
   * when the submission is no longer in the local db, but the user is updating rather than creating
   * a new submission. In these cases creation metadata is unknown, so empty audit info is used.
   */
  private fun fallbackSubmission(mutation: SubmissionMutation): SingleSource<SubmissionEntity> =
    SingleSource {
      it.onSuccess(mutation.toLocalDataStoreObject(AuditInfo(User("", "", ""))))
    }

  private fun insertFromMutation(mutation: SubmissionMutation, user: User): Completable =
    submissionDao
      .insertOrUpdate(mutation.toLocalDataStoreObject(AuditInfo(user)))
      .doOnSubscribe { Timber.v("Inserting submission: $mutation") }
      .subscribeOn(schedulers.io())

  private fun mergeSubmission(
    job: Job,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>
  ): Completable {
    if (mutations.isEmpty()) {
      return submissionDao.insertOrUpdate(submission)
    }
    val lastMutation = mutations[mutations.size - 1]
    Preconditions.checkNotNull(lastMutation, "Could not get last mutation")
    return userStore
      .getUser(lastMutation.userId)
      .map { user -> commitMutations(job, submission, mutations, user) }
      .flatMapCompletable { submissionDao.insertOrUpdate(it) }
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

    return submission.apply {
      responses = ResponseMapConverter.toString(commitMutations(job, this, mutations))
      lastModified = AuditInfoEntity(UserDetails.fromUser(user), clientTimestamp)
      Timber.v("Merged submission $this")
    }
  }

  private fun commitMutations(
    job: Job?,
    submission: SubmissionEntity,
    mutations: List<SubmissionMutationEntity>
  ): TaskDataMap {
    val responseMap = ResponseMapConverter.fromString(job!!, submission.responses)
    val deltas = ImmutableList.builder<TaskDataDelta>()
    for (mutation in mutations) {
      // Merge changes to responses.
      deltas.addAll(ResponseDeltasConverter.fromString(job, mutation.responseDeltas))
    }
    return responseMap.copyWithDeltas(deltas.build())
  }

  private fun toSubmissions(
    locationOfInterest: LocationOfInterest,
    submissionEntities: List<SubmissionEntity>
  ): ImmutableList<Submission> =
    submissionEntities
      .flatMap { logAndSkip { it.toModelObject(locationOfInterest) } }
      .toImmutableList()

  override fun deleteSubmission(submissionId: String): Completable =
    submissionDao
      .findById(submissionId)
      .toSingle()
      .doOnSubscribe { Timber.d("Deleting local submission : $submissionId") }
      .flatMapCompletable { submissionDao.delete(it) }
      .subscribeOn(schedulers.io())

  override fun getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
    survey: Survey,
    locationOfInterestId: String,
    vararg allowedStates: MutationEntitySyncStatus
  ): Flowable<ImmutableList<SubmissionMutation>> =
    submissionMutationDao
      .findByLocationOfInterestIdOnceAndStream(locationOfInterestId, *allowedStates)
      .map { list: List<SubmissionMutationEntity> ->
        list.map { it.toModelObject(survey) }.toImmutableList()
      }

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

  override fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus
  ): Single<List<SubmissionMutationEntity>> =
    submissionMutationDao.findByLocationOfInterestId(id, *states)
}
