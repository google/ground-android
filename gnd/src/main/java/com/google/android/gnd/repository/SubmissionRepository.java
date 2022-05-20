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

package com.google.android.gnd.repository;

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.mutation.Mutation.SyncStatus;
import com.google.android.gnd.model.mutation.Mutation.Type;
import com.google.android.gnd.model.mutation.SubmissionMutation;
import com.google.android.gnd.model.submission.ResponseDelta;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.gnd.persistence.remote.NotFoundException;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Coordinates persistence and retrieval of {@link Submission} instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
public class SubmissionRepository {

  private static final long LOAD_REMOTE_SUBMISSIONS_TIMEOUT_SECS = 15;

  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final FeatureRepository featureRepository;
  private final DataSyncWorkManager dataSyncWorkManager;
  private final OfflineUuidGenerator uuidGenerator;
  private final AuthenticationManager authManager;

  @Inject
  public SubmissionRepository(
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore,
      FeatureRepository featureRepository,
      DataSyncWorkManager dataSyncWorkManager,
      OfflineUuidGenerator uuidGenerator,
      AuthenticationManager authManager) {

    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.featureRepository = featureRepository;
    this.dataSyncWorkManager = dataSyncWorkManager;
    this.uuidGenerator = uuidGenerator;
    this.authManager = authManager;
  }

  /**
   * Retrieves the submissions or the specified project, feature, and form.
   *
   * <ol>
   *   <li>Attempt to sync remote submission changes to the local data store. If network is not
   *       available or operation times out, this step is skipped.
   *   <li>Relevant submissions are returned directly from the local data store.
   * </ol>
   */
  @Cold
  public Single<ImmutableList<Submission>> getSubmissions(
      String projectId, String featureId, String formId) {
    // TODO: Only fetch first n fields.
    return featureRepository
        .getFeature(projectId, featureId)
        .flatMap(feature -> getSubmissions(feature, formId));
  }

  @Cold
  private Single<ImmutableList<Submission>> getSubmissions(Feature feature, String formId) {
    Completable remoteSync =
        remoteDataStore
            .loadSubmissions(feature)
            .timeout(LOAD_REMOTE_SUBMISSIONS_TIMEOUT_SECS, TimeUnit.SECONDS)
            .doOnError(t -> Timber.e(t, "Submission sync timed out"))
            .flatMapCompletable(this::mergeRemoteSubmissions)
            .onErrorComplete();
    return remoteSync.andThen(localDataStore.getSubmissions(feature, formId));
  }

  @Cold
  private Completable mergeRemoteSubmissions(ImmutableList<ValueOrError<Submission>> submissions) {
    return Observable.fromIterable(submissions)
        .doOnNext(voe -> voe.error().ifPresent(t -> Timber.e(t, "Skipping bad submission")))
        .compose(ValueOrError::ignoreErrors)
        .flatMapCompletable(localDataStore::mergeSubmission);
  }

  @Cold
  public Single<Submission> getSubmission(String projectId, String featureId, String submissionId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    return featureRepository
        .getFeature(projectId, featureId)
        .flatMap(
            feature ->
                localDataStore
                    .getSubmission(feature, submissionId)
                    .switchIfEmpty(
                        Single.error(() -> new NotFoundException("Submission " + submissionId))));
  }

  @Cold
  public Single<Submission> createSubmission(String projectId, String featureId, String formId) {
    // TODO: Handle invalid formId.
    AuditInfo auditInfo = AuditInfo.now(authManager.getCurrentUser());
    return featureRepository
        .getFeature(projectId, featureId)
        .map(
            feature ->
                Submission.newBuilder()
                    .setId(uuidGenerator.generateUuid())
                    .setProject(feature.getProject())
                    .setFeature(feature)
                    .setForm(feature.getLayer().getForm(formId).get())
                    .setCreated(auditInfo)
                    .setLastModified(auditInfo)
                    .build());
  }

  @Cold
  public Completable deleteSubmission(Submission submission) {
    SubmissionMutation submissionMutation =
        SubmissionMutation.builder()
            .setSubmissionId(submission.getId())
            .setForm(submission.getForm())
            .setResponseDeltas(ImmutableList.of())
            .setType(Type.DELETE)
            .setSyncStatus(SyncStatus.PENDING)
            .setProjectId(submission.getProject().getId())
            .setFeatureId(submission.getFeature().getId())
            .setLayerId(submission.getFeature().getLayer().getId())
            .setClientTimestamp(new Date())
            .setUserId(authManager.getCurrentUser().getId())
            .build();
    return applyAndEnqueue(submissionMutation);
  }

  @Cold
  public Completable createOrUpdateSubmission(
      Submission submission, ImmutableList<ResponseDelta> responseDeltas, boolean isNew) {
    SubmissionMutation submissionMutation =
        SubmissionMutation.builder()
            .setSubmissionId(submission.getId())
            .setForm(submission.getForm())
            .setResponseDeltas(responseDeltas)
            .setType(isNew ? SubmissionMutation.Type.CREATE : SubmissionMutation.Type.UPDATE)
            .setSyncStatus(SyncStatus.PENDING)
            .setProjectId(submission.getProject().getId())
            .setFeatureId(submission.getFeature().getId())
            .setLayerId(submission.getFeature().getLayer().getId())
            .setClientTimestamp(new Date())
            .setUserId(authManager.getCurrentUser().getId())
            .build();
    return applyAndEnqueue(submissionMutation);
  }

  @Cold
  private Completable applyAndEnqueue(SubmissionMutation mutation) {
    return localDataStore
        .applyAndEnqueue(mutation)
        .andThen(dataSyncWorkManager.enqueueSyncWorker(mutation.getFeatureId()));
  }

  /**
   * Returns all {@link SubmissionMutation} instances for a given feature which have not yet been
   * marked as {@link SyncStatus#COMPLETED}, including pending, in progress, and failed mutations. A
   * new list is emitted on each subsequent change.
   */
  public Flowable<ImmutableList<SubmissionMutation>> getIncompleteSubmissionMutationsOnceAndStream(
      Project project, String featureId) {
    return localDataStore.getSubmissionMutationsByFeatureIdOnceAndStream(
        project,
        featureId,
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS,
        MutationEntitySyncStatus.FAILED);
  }
}
