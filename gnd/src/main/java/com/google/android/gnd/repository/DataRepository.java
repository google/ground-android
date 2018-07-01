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

import android.annotation.SuppressLint;
import android.util.Log;
import com.google.android.gnd.service.DatastoreEvent;
import com.google.android.gnd.service.RemoteDataService;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
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

  public Flowable<Resource<Project>> getActiveProject() {
    // TODO: On subscribe and project in cache not loaded, read last active project from local db.
    return activeProjectSubject
        .toFlowable(BackpressureStrategy.LATEST)
        .startWith(cache.getActiveProject().map(Resource::loaded).orElse(Resource.notLoaded()));
  }

  public Flowable<Resource<List<Project>>> loadProjectSummaries() {
    // TODO: Get from load db if network connection not available or remote times out.
    return remoteDataService
        .loadProjectSummaries()
        .map(Resource::loaded)
        .onErrorReturn(Resource::error)
        .toFlowable()
        .startWith(Resource.loading());
  }

  @SuppressLint("CheckResult")
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

  // TODO: Only return data needed to render place PLPs.
  // TODO: Wrap Place in Resource<>.
  public Flowable<ImmutableSet<Place>> getPlaceVectors(Project project) {
    return remoteDataService
        .observePlaces(project)
        .doOnNext(this::updateCache)
        .map(__ -> cache.getPlaces());
  }

  private void updateCache(DatastoreEvent<Place> event) {
    event.getEntity().ifPresentOrElse(cache::putPlace, () -> cache.removePlace(event.getId()));
  }

  //  public Place update(PlaceUpdate placeUpdate) {
  //    projectStateObservable.getValue().getActiveProject()
  //
  //    return remoteDataService.update(activeProject.getId(), placeUpdate);
  //  }

  public Single<List<Record>> loadRecordSummaries(Project project, String placeId) {
    // TODO: Only fetch first n fields.
    return remoteDataService.loadRecordData(project.getId(), placeId);
  }
}
