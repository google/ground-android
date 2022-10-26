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
package com.google.android.ground.repository

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.sync.DataSyncWorkManager
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.common.collect.ImmutableList
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import timber.log.Timber

private const val LOAD_REMOTE_SUBMISSIONS_TIMEOUT_SECS: Long = 15

/**
 * Coordinates persistence and retrieval of [Submission] instances from remote, local, and in memory
 * data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
class SubmissionRepository
@Inject
constructor(
  private val localDataStore: LocalDataStore,
  private val remoteDataStore: RemoteDataStore,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val dataSyncWorkManager: DataSyncWorkManager,
  private val uuidGenerator: OfflineUuidGenerator,
  private val authManager: AuthenticationManager
) {

  /**
   * Retrieves the submissions or the specified survey, location of interest, and task.
   *
   * <ol> <li>Attempt to sync remote submission changes to the local data store. If network is not
   * ```
   *       available or operation times out, this step is skipped.
   * ```
   * <li>Relevant submissions are returned directly from the local data store. </ol>
   */
  fun getSubmissions(
    surveyId: String,
    locationOfInterestId: String,
    taskId: String
  ): @Cold Single<ImmutableList<Submission>> =
    // TODO: Only fetch first n fields.
    locationOfInterestRepository.getLocationOfInterest(surveyId, locationOfInterestId).flatMap {
      locationOfInterest: LocationOfInterest ->
      getSubmissions(locationOfInterest, taskId)
    }

  private fun getSubmissions(
    locationOfInterest: LocationOfInterest,
    taskId: String
  ): @Cold Single<ImmutableList<Submission>> {
    val remoteSync =
      remoteDataStore
        .loadSubmissions(locationOfInterest)
        .timeout(LOAD_REMOTE_SUBMISSIONS_TIMEOUT_SECS, TimeUnit.SECONDS)
        .doOnError { Timber.e(it, "Submission sync timed out") }
        .flatMapCompletable { submissions: ImmutableList<Result<Submission>> ->
          mergeRemoteSubmissions(submissions)
        }
        .onErrorComplete()
    return remoteSync.andThen(localDataStore.getSubmissions(locationOfInterest, taskId))
  }

  private fun mergeRemoteSubmissions(
    submissions: ImmutableList<Result<Submission>>
  ): @Cold Completable {
    return Observable.fromIterable(submissions)
      .doOnNext { result: Result<Submission> ->
        if (result.isFailure) {
          Timber.e(result.exceptionOrNull(), "Skipping bad submission")
        }
      }
      .filter { it.isSuccess }
      .map { it.getOrThrow() }
      .flatMapCompletable { localDataStore.mergeSubmission(it) }
  }

  fun getSubmission(
    surveyId: String,
    locationOfInterestId: String,
    submissionId: String
  ): @Cold Single<Submission> =
    // TODO: Store and retrieve latest edits from cache and/or db.
    locationOfInterestRepository.getLocationOfInterest(surveyId, locationOfInterestId).flatMap {
      locationOfInterest ->
      localDataStore
        .getSubmission(locationOfInterest, submissionId)
        .switchIfEmpty(Single.error { NotFoundException("Submission $submissionId") })
    }

  fun createSubmission(
    surveyId: String,
    locationOfInterestId: String,
    jobId: String
  ): @Cold Single<Submission> {
    // TODO: Very jobId == loi job id.
    val auditInfo = AuditInfo(authManager.currentUser)
    return locationOfInterestRepository.getLocationOfInterest(surveyId, locationOfInterestId).map {
      locationOfInterest: LocationOfInterest ->
      Submission(
        uuidGenerator.generateUuid(),
        locationOfInterest.surveyId,
        locationOfInterest,
        locationOfInterest.job,
        auditInfo,
        auditInfo
      )
    }
  }

  fun deleteSubmission(submission: Submission): @Cold Completable =
    applyAndEnqueue(
      SubmissionMutation(
        job = submission.job,
        submissionId = submission.id,
        type = Mutation.Type.DELETE,
        syncStatus = SyncStatus.PENDING,
        surveyId = submission.surveyId,
        locationOfInterestId = submission.locationOfInterest.id,
        userId = authManager.currentUser.id
      )
    )

  fun createOrUpdateSubmission(
    submission: Submission,
    taskDataDeltas: ImmutableList<TaskDataDelta>,
    isNew: Boolean
  ): @Cold Completable =
    applyAndEnqueue(
      SubmissionMutation(
        job = submission.job,
        submissionId = submission.id,
        taskDataDeltas = taskDataDeltas,
        type = if (isNew) Mutation.Type.CREATE else Mutation.Type.UPDATE,
        syncStatus = SyncStatus.PENDING,
        surveyId = submission.surveyId,
        locationOfInterestId = submission.locationOfInterest.id,
        userId = authManager.currentUser.id
      )
    )

  private fun applyAndEnqueue(mutation: SubmissionMutation): @Cold Completable =
    localDataStore
      .applyAndEnqueue(mutation)
      .andThen(dataSyncWorkManager.enqueueSyncWorker(mutation.locationOfInterestId))

  /**
   * Returns all [SubmissionMutation] instances for a given location of interest which have not yet
   * been marked as [SyncStatus.COMPLETED], including pending, in progress, and failed mutations. A
   * new list is emitted on each subsequent change.
   */
  fun getIncompleteSubmissionMutationsOnceAndStream(
    surveyId: String,
    locationOfInterestId: String
  ): Flowable<ImmutableList<SubmissionMutation>> =
    localDataStore.getSurveyById(surveyId).toFlowable().flatMap {
      localDataStore.getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
        it,
        locationOfInterestId,
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS,
        MutationEntitySyncStatus.FAILED
      )
    }
}
