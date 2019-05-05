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

import android.util.Log;
import com.google.android.gnd.service.DataStoreEvent;
import com.google.android.gnd.service.RemoteDataStore;
import com.google.android.gnd.service.firestore.DocumentNotFoundException;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.FeatureUpdate.RecordUpdate.ResponseUpdate;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataRepository {
  private static final String TAG = DataRepository.class.getSimpleName();

  // TODO: Implement local data persistence.
  // For cached data, InMemoryCache is the source of truth that the repository subscribes to.
  // For non-cached data, the local database will be the source of truth.
  // Remote data is written to the database, and then optionally to the InMemoryCache.
  private final InMemoryCache cache;
  private final RemoteDataStore remoteDataStore;
  private final Subject<Resource<Project>> activeProjectSubject;

  @Inject
  public DataRepository(RemoteDataStore remoteDataStore, InMemoryCache cache) {
    this.remoteDataStore = remoteDataStore;
    this.cache = cache;
    this.activeProjectSubject = BehaviorSubject.create();
  }

  public Flowable<Resource<Project>> getActiveProject() {
    // TODO: On subscribe and project in cache not loaded, read last active project from local db.
    return activeProjectSubject
        .startWith(cache.getActiveProject().map(Resource::loaded).orElse(Resource.notLoaded()))
        .toFlowable(BackpressureStrategy.LATEST);
  }

  public Completable activateProject(String projectId) {
    Log.d(TAG, " Activating project " + projectId);
    return remoteDataStore
        .loadProject(projectId)
        .doOnSubscribe(__ -> activeProjectSubject.onNext(Resource.loading()))
        .doOnSuccess(this::onProjectLoaded)
        .toCompletable();
  }

  private void onProjectLoaded(Project project) {
    cache.setActiveProject(project);
    activeProjectSubject.onNext(Resource.loaded(project));
  }

  public Observable<Resource<List<Project>>> getProjectSummaries(User user) {
    // TODO: Get from load db if network connection not available or remote times out.
    return remoteDataStore
        .loadProjectSummaries(user)
        .map(Resource::loaded)
        .onErrorReturn(Resource::error)
        .toObservable()
        .startWith(Resource.loading());
  }

  // TODO: Only return data needed to render feature PLPs.
  // TODO: Wrap Feature in Resource<>.
  // TODO: Accept id instead.
  public Flowable<ImmutableSet<Feature>> getFeatureVectorStream(Project project) {
    return remoteDataStore
        .getFeatureVectorStream(project)
        .doOnNext(this::onRemoteFeatureVectorChange)
        .map(__ -> cache.getFeatures());
  }

  private void onRemoteFeatureVectorChange(DataStoreEvent<Feature> event) {
    event.getEntity().ifPresentOrElse(cache::putFeature, () -> cache.removeFeature(event.getId()));
  }

  // TODO: Return Resource.
  public Single<List<Record>> getRecordSummaries(String projectId, String featureId) {
    // TODO: Only fetch first n fields.
    // TODO: Also load from db.
    return getFeature(projectId, featureId)
        .flatMap(feature -> remoteDataStore.loadRecordSummaries(feature));
  }

  private Single<Feature> getFeature(String projectId, String featureId) {
    // TODO: Load from db if not in cache.
    return getProject(projectId)
        .flatMap(
            project ->
                cache
                    .getFeature(featureId)
                    .map(Single::just)
                    .orElse(Single.error(new DocumentNotFoundException())));
  }

  public Flowable<Resource<Record>> getRecordDetails(
      String projectId, String featureId, String recordId) {
    return getFeature(projectId, featureId)
        .flatMap(feature -> remoteDataStore.loadRecordDetails(feature, recordId))
        .map(Resource::loaded)
        .onErrorReturn(Resource::error)
        .toFlowable();
  }

  public Single<Resource<Record>> getRecordSnapshot(
      String projectId, String featureId, String recordId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    return getFeature(projectId, featureId)
        .flatMap(feature -> remoteDataStore.loadRecordDetails(feature, recordId))
        .map(Resource::loaded)
        .onErrorReturn(Resource::error);
  }

  public Single<Record> createRecord(String projectId, String featureId, String formId) {
    // TODO: Handle invalid formId.
    return getFeature(projectId, featureId)
        .map(
            feature ->
                Record.newBuilder()
                    .setProject(feature.getProject())
                    .setFeature(feature)
                    .setForm(feature.getFeatureType().getForm(formId).get())
                    .build());
  }

  private Single<Project> getProject(String projectId) {
    // TODO: Try to load from db if network not available or times out.
    return cache
        .getActiveProject()
        .filter(p -> projectId.equals(p.getId()))
        .map(Single::just)
        .orElse(remoteDataStore.loadProject(projectId));
  }

  public Observable<Resource<Record>> saveChanges(
      Record record, ImmutableList<ResponseUpdate> updates, User user) {
    record = attachUser(record, user);
    return remoteDataStore
        .saveChanges(record, updates)
        .map(Resource::saved)
        .toObservable()
        .startWith(Resource.saving(record));
  }

  private Record attachUser(Record record, User user) {
    Record.Builder builder = record.toBuilder();
    // TODO: Set these creation time instead.
    if (record.getId() == null && record.getCreatedBy() == null) {
      builder.setCreatedBy(user);
    }
    builder.setModifiedBy(user);
    return builder.build();
  }

  public Completable saveFeature(Feature feature) {
    return remoteDataStore.saveFeature(feature);
  }

  public void clearActiveProject() {
    cache.clearActiveProject();
    activeProjectSubject.onNext(Resource.notLoaded());
  }
}
