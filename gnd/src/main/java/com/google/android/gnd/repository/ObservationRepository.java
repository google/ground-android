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
import com.google.android.gnd.model.Mutation.Type;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.remote.NotFoundException;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Coordinates persistence and retrieval of {@link Observation} instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
public class ObservationRepository {

  private static final long LOAD_REMOTE_OBSERVATIONS_TIMEOUT_SECS = 5;

  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final FeatureRepository featureRepository;
  private final DataSyncWorkManager dataSyncWorkManager;
  private final OfflineUuidGenerator uuidGenerator;
  private final AuthenticationManager authManager;

  @Inject
  public ObservationRepository(
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
   * Retrieves the observations or the specified project, feature, and form.
   *
   * <ol>
   *   <li>Attempt to sync remote observation changes to the local data store. If network is not
   *       available or operation times out, this step is skipped.
   *   <li>Relevant observations are returned directly from the local data store.
   * </ol>
   */
  @Cold
  public Single<ImmutableList<Observation>> getObservations(
      String projectId, String featureId, String formId) {
    // TODO: Only fetch first n fields.
    return featureRepository
        .getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(() -> new NotFoundException("Feature " + featureId)))
        .flatMap(feature -> getObservations(feature, formId));
  }

  @Cold
  private Single<ImmutableList<Observation>> getObservations(Feature feature, String formId) {
    Completable remoteSync =
        remoteDataStore
            .loadObservations(feature)
            .timeout(LOAD_REMOTE_OBSERVATIONS_TIMEOUT_SECS, TimeUnit.SECONDS)
            .doOnError(t -> Timber.e(t, "Observation sync timed out"))
            .flatMapCompletable(this::mergeRemoteObservations)
            .onErrorComplete();
    return remoteSync.andThen(localDataStore.getObservations(feature, formId));
  }

  @Cold
  private Completable mergeRemoteObservations(
      ImmutableList<ValueOrError<Observation>> observations) {
    return Observable.fromIterable(observations)
        .doOnNext(voe -> voe.error().ifPresent(t -> Timber.e(t, "Skipping bad observation")))
        .compose(ValueOrError::ignoreErrors)
        .flatMapCompletable(localDataStore::mergeObservation);
  }

  @Cold
  public Single<Observation> getObservation(
      String projectId, String featureId, String observationId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    return featureRepository
        .getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(() -> new NotFoundException("Feature " + featureId)))
        .flatMap(
            feature ->
                localDataStore
                    .getObservation(feature, observationId)
                    .switchIfEmpty(
                        Single.error(() -> new NotFoundException("Observation " + observationId))));
  }

  @Cold
  public Single<Observation> createObservation(String projectId, String featureId, String formId) {
    // TODO: Handle invalid formId.
    AuditInfo auditInfo = AuditInfo.now(authManager.getCurrentUser());
    return featureRepository
        .getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(() -> new NotFoundException("Feature " + featureId)))
        .map(
            feature ->
                Observation.newBuilder()
                    .setId(uuidGenerator.generateUuid())
                    .setProject(feature.getProject())
                    .setFeature(feature)
                    .setForm(feature.getLayer().getForm(formId).get())
                    .setCreated(auditInfo)
                    .setLastModified(auditInfo)
                    .build());
  }

  @Cold
  public Completable deleteObservation(Observation observation) {
    ObservationMutation observationMutation =
        ObservationMutation.builder()
            .setType(Type.DELETE)
            .setProjectId(observation.getProject().getId())
            .setFeatureId(observation.getFeature().getId())
            .setLayerId(observation.getFeature().getLayer().getId())
            .setObservationId(observation.getId())
            .setForm(observation.getForm())
            .setResponseDeltas(ImmutableList.of())
            .setClientTimestamp(new Date())
            .setUserId(authManager.getCurrentUser().getId())
            .build();
    return applyAndEnqueue(observationMutation);
  }

  @Cold
  public Completable addObservationMutation(
      Observation observation, ImmutableList<ResponseDelta> responseDeltas, boolean isNew) {
    ObservationMutation observationMutation =
        ObservationMutation.builder()
            .setType(isNew ? ObservationMutation.Type.CREATE : ObservationMutation.Type.UPDATE)
            .setProjectId(observation.getProject().getId())
            .setFeatureId(observation.getFeature().getId())
            .setLayerId(observation.getFeature().getLayer().getId())
            .setObservationId(observation.getId())
            .setForm(observation.getForm())
            .setResponseDeltas(responseDeltas)
            .setClientTimestamp(new Date())
            .setUserId(authManager.getCurrentUser().getId())
            .build();
    return applyAndEnqueue(observationMutation);
  }

  @Cold
  private Completable applyAndEnqueue(ObservationMutation mutation) {
    return localDataStore
        .applyAndEnqueue(mutation)
        .andThen(dataSyncWorkManager.enqueueSyncWorker(mutation.getFeatureId()));
  }
}
