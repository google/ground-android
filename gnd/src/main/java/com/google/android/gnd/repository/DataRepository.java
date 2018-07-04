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
import com.google.android.gnd.service.DatastoreEvent;
import com.google.android.gnd.service.RemoteDataService;
import com.google.android.gnd.service.firestore.DocumentNotFoundException;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
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
  private final RemoteDataService remoteDataService;
  private final Subject<Resource<Project>> activeProjectSubject;

  @Inject
  public DataRepository(RemoteDataService remoteDataService, InMemoryCache cache) {
    this.remoteDataService = remoteDataService;
    this.cache = cache;
    this.activeProjectSubject = PublishSubject.create();
  }

  public Flowable<Resource<Project>> getActiveProjectStream() {
    // TODO: On subscribe and project in cache not loaded, read last active project from local db.
    return activeProjectSubject
        .toFlowable(BackpressureStrategy.LATEST)
        .startWith(cache.getActiveProject().map(Resource::loaded).orElse(Resource.notLoaded()));
  }

  public Completable activateProject(String projectId) {
    Log.d(TAG, " Activating project " + projectId);
    return remoteDataService
        .loadProject(projectId)
        .doOnSubscribe(__ -> activeProjectSubject.onNext(Resource.loading()))
        .doOnSuccess(this::onProjectLoaded)
        .toCompletable();
  }

  private void onProjectLoaded(Project project) {
    cache.setActiveProject(project);
    activeProjectSubject.onNext(Resource.loaded(project));
  }

  public Flowable<Resource<List<Project>>> getProjectSummaries() {
    // TODO: Get from load db if network connection not available or remote times out.
    return remoteDataService
        .loadProjectSummaries()
        .map(Resource::loaded)
        .onErrorReturn(Resource::error)
        .toFlowable()
        .startWith(Resource.loading());
  }

  // TODO: Only return data needed to render place PLPs.
  // TODO: Wrap Place in Resource<>.
  // TODO: Accept id instead.
  public Flowable<ImmutableSet<Place>> getPlaceVectorStream(Project project) {
    return remoteDataService
        .getPlaceVectorStream(project)
        .doOnNext(this::onRemotePlaceVectorChange)
        .map(__ -> cache.getPlaces());
  }

  private void onRemotePlaceVectorChange(DatastoreEvent<Place> event) {
    event.getEntity().ifPresentOrElse(cache::putPlace, () -> cache.removePlace(event.getId()));
  }

  //  public Place update(PlaceUpdate placeUpdate) {
  //    projectStateObservable.getValue().getActiveProjectStream()
  //
  //    return remoteDataService.update(activeProject.getId(), placeUpdate);
  //  }

  // TODO: Return Resource.
  public Flowable<List<Record>> getRecordSummaries(String projectId, String placeId) {
    // TODO: Only fetch first n fields.
    // TODO: Also load from db.
    return getPlace(projectId, placeId)
        .flatMap(place -> remoteDataService.loadRecordSummaries(place))
        .toFlowable();
  }

  private Single<Place> getPlace(String projectId, String placeId) {
    // TODO: Load from db if not in cache.
    return getProject(projectId)
        .flatMap(
            project ->
                cache
                    .getPlace(placeId)
                    .map(Single::just)
                    .orElse(Single.error(new DocumentNotFoundException())));
  }

  public Flowable<Resource<Record>> getRecordDetails(
      String projectId, String placeId, String recordId) {
    return getPlace(projectId, placeId)
        .flatMap(place -> remoteDataService.loadRecordDetails(place, recordId))
        .map(Resource::loaded)
        .onErrorReturn(Resource::error)
        .toFlowable();
  }

  public Single<Resource<Record>> getRecordSnapshot(
    String projectId, String placeId, String recordId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    return getPlace(projectId, placeId)
      .flatMap(place -> remoteDataService.loadRecordDetails(place, recordId))
      .map(Resource::loaded)
      .onErrorReturn(Resource::error);
  }

  private Single<Project> getProject(String projectId) {
    // TODO: Try to load from db if network not available or times out.
    return cache
        .getActiveProject()
        .filter(p -> projectId.equals(p.getId()))
        .map(Single::just)
        .orElse(remoteDataService.loadProject(projectId));
  }

  public Completable saveChanges(Record record, ImmutableList<ValueUpdate> updates) {
    return remoteDataService.saveChanges(record, updates);
  }
}
