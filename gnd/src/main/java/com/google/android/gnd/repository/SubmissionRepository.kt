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
package com.google.android.gnd.repository

import com.google.android.gnd.model.AuditInfo
import com.google.android.gnd.model.Survey
import com.google.android.gnd.model.feature.Feature
import com.google.android.gnd.model.mutation.Mutation
import com.google.android.gnd.model.mutation.Mutation.SyncStatus
import com.google.android.gnd.model.mutation.SubmissionMutation
import com.google.android.gnd.model.mutation.SubmissionMutation.Companion.builder
import com.google.android.gnd.model.submission.ResponseDelta
import com.google.android.gnd.model.submission.Submission
import com.google.android.gnd.persistence.local.LocalDataStore
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.gnd.persistence.remote.NotFoundException
import com.google.android.gnd.persistence.remote.RemoteDataStore
import com.google.android.gnd.persistence.sync.DataSyncWorkManager
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator
import com.google.android.gnd.rx.ValueOrError
import com.google.android.gnd.rx.annotations.Cold
import com.google.android.gnd.system.auth.AuthenticationManager
import com.google.common.collect.ImmutableList
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val LOAD_REMOTE_SUBMISSIONS_TIMEOUT_SECS: Long = 15

/**
 * Coordinates persistence and retrieval of [Submission] instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
class SubmissionRepository @Inject constructor(
    private val localDataStore: LocalDataStore,
    private val remoteDataStore: RemoteDataStore,
    private val featureRepository: FeatureRepository,
    private val dataSyncWorkManager: DataSyncWorkManager,
    private val uuidGenerator: OfflineUuidGenerator,
    private val authManager: AuthenticationManager
) {

    /**
     * Retrieves the submissions or the specified survey, feature, and task.
     *
     * <ol>
     *   <li>Attempt to sync remote submission changes to the local data store. If network is not
     *       available or operation times out, this step is skipped.
     *   <li>Relevant submissions are returned directly from the local data store.
     * </ol>
     */
    fun getSubmissions(
        surveyId: String,
        featureId: String,
        taskId: String
    ): @Cold Single<ImmutableList<Submission>> =
        // TODO: Only fetch first n fields.
        featureRepository
            .getFeature(surveyId, featureId)
            .flatMap { feature: Feature<*> -> getSubmissions(feature, taskId) }

    private fun getSubmissions(
        feature: Feature<*>,
        taskId: String
    ): @Cold Single<ImmutableList<Submission>> {
        val remoteSync = remoteDataStore
            .loadSubmissions(feature)
            .timeout(LOAD_REMOTE_SUBMISSIONS_TIMEOUT_SECS, TimeUnit.SECONDS)
            .doOnError { Timber.e(it, "Submission sync timed out") }
            .flatMapCompletable { submissions: ImmutableList<ValueOrError<Submission>> ->
                mergeRemoteSubmissions(submissions)
            }
            .onErrorComplete()
        return remoteSync.andThen(localDataStore.getSubmissions(feature, taskId))
    }

    private fun mergeRemoteSubmissions(submissions: ImmutableList<ValueOrError<Submission>>): @Cold Completable {
        return Observable.fromIterable(submissions)
            .doOnNext { voe: ValueOrError<Submission> ->
                voe.error().ifPresent { Timber.e(it, "Skipping bad submission") }
            }
            .compose { ValueOrError.ignoreErrors(it) }
            .flatMapCompletable { submission: Submission ->
                localDataStore.mergeSubmission(submission)
            }
    }

    fun getSubmission(
        surveyId: String,
        featureId: String,
        submissionId: String
    ): @Cold Single<Submission> =
        // TODO: Store and retrieve latest edits from cache and/or db.
        featureRepository
            .getFeature(surveyId, featureId)
            .flatMap { feature: Feature<*> ->
                localDataStore
                    .getSubmission(feature, submissionId)
                    .switchIfEmpty(Single.error { NotFoundException("Submission $submissionId") })
            }

    fun createSubmission(
        surveyId: String,
        featureId: String,
        taskId: String
    ): @Cold Single<Submission> {
        // TODO: Handle invalid taskId.
        val auditInfo = AuditInfo.now(authManager.currentUser)
        return featureRepository
            .getFeature(surveyId, featureId)
            .map { feature: Feature<*> ->
                Submission.newBuilder()
                    .setId(uuidGenerator.generateUuid())
                    .setSurvey(feature.survey)
                    .setFeature(feature)
                    .setTask(feature.job.getTask(taskId).get())
                    .setCreated(auditInfo)
                    .setLastModified(auditInfo)
                    .build()
            }
    }

    fun deleteSubmission(submission: Submission): @Cold Completable =
        applyAndEnqueue(
            builder()
                .setSubmissionId(submission.id)
                .setTask(submission.task)
                .setResponseDeltas(ImmutableList.of())
                .setType(Mutation.Type.DELETE)
                .setSyncStatus(SyncStatus.PENDING)
                .setSurveyId(submission.survey.id)
                .setFeatureId(submission.feature.id)
                .setJobId(submission.feature.job.id)
                .setClientTimestamp(Date())
                .setUserId(authManager.currentUser.id)
                .build()
        )

    fun createOrUpdateSubmission(
        submission: Submission, responseDeltas: ImmutableList<ResponseDelta>, isNew: Boolean
    ): @Cold Completable =
        applyAndEnqueue(
            builder()
                .setSubmissionId(submission.id)
                .setTask(submission.task)
                .setResponseDeltas(responseDeltas)
                .setType(if (isNew) Mutation.Type.CREATE else Mutation.Type.UPDATE)
                .setSyncStatus(SyncStatus.PENDING)
                .setSurveyId(submission.survey.id)
                .setFeatureId(submission.feature.id)
                .setJobId(submission.feature.job.id)
                .setClientTimestamp(Date())
                .setUserId(authManager.currentUser.id)
                .build()
        )

    private fun applyAndEnqueue(mutation: SubmissionMutation): @Cold Completable =
        localDataStore
            .applyAndEnqueue(mutation)
            .andThen(dataSyncWorkManager.enqueueSyncWorker(mutation.featureId))

    /**
     * Returns all [SubmissionMutation] instances for a given feature which have not yet been
     * marked as [SyncStatus.COMPLETED], including pending, in progress, and failed mutations. A
     * new list is emitted on each subsequent change.
     */
    fun getIncompleteSubmissionMutationsOnceAndStream(
        survey: Survey, featureId: String
    ): Flowable<ImmutableList<SubmissionMutation>> =
        localDataStore.getSubmissionMutationsByFeatureIdOnceAndStream(
            survey,
            featureId,
            MutationEntitySyncStatus.PENDING,
            MutationEntitySyncStatus.IN_PROGRESS,
            MutationEntitySyncStatus.FAILED
        )
}