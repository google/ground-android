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
import com.google.android.gnd.service.DataService;
import com.google.android.gnd.service.DatastoreEvent;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableSet;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GndDataRepository {

  private final DataService dataService;
  private final InMemoryCache inMemoryCache;
  private BehaviorSubject<ProjectState> projectStateObservable;

  @Inject
  public GndDataRepository(DataService dataService, InMemoryCache inMemoryCache) {
    this.dataService = dataService;
    this.inMemoryCache = inMemoryCache;
    projectStateObservable = BehaviorSubject.createDefault(ProjectState.inactive());
  }

  public Flowable<ProjectState> getProjectState() {
    return projectStateObservable.toFlowable(BackpressureStrategy.LATEST);
  }

  @SuppressLint("CheckResult")
  public Completable activateProject(String projectId) {
    projectStateObservable.onNext(ProjectState.loading());
    // TODO: Make loadProject return Completable instead of Maybe?
    return dataService
        .loadProject(projectId)
        .doOnSuccess(this::onProjectLoaded)
        .flatMapCompletable(
            p ->
                p == null
                    ? Completable.error(new IOException("Error loading project"))
                    : Completable.complete());
  }

  private void onProjectLoaded(Project project) {
    inMemoryCache.clear();
    projectStateObservable.onNext(ProjectState.activated(project, getPlaces(project)));
  }

  private Flowable<ImmutableSet<Place>> getPlaces(Project project) {
    return dataService
        .observePlaces(project)
        .doOnNext(this::updateCache)
        .map(__ -> inMemoryCache.getPlaces());
  }

  private void updateCache(DatastoreEvent<Place> event) {
    event
        .getEntity()
        .ifPresentOrElse(inMemoryCache::putPlace, () -> inMemoryCache.removePlace(event.getId()));
  }

  //  public Place update(PlaceUpdate placeUpdate) {
  //    projectStateObservable.getValue().getActiveProject()
  //
  //    return dataService.update(activeProject.getId(), placeUpdate);
  //  }

  public Single<List<Record>> loadRecordSummaries(Project project, String placeId) {
    // TODO: Only fetch first n fields.
    return dataService.loadRecordData(project.getId(), placeId);
  }

  public Single<List<Project>> loadProjectSummaries() {
    return dataService.loadProjectSummaries();
  }
}
