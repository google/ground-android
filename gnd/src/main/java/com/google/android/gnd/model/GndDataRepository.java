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

package com.google.android.gnd.model;

import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.service.DataService;
import com.google.android.gnd.service.DatastoreEvent;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.List;
import java8.util.Optional;
import java8.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO: Move to its own package ("repository"?).
@Singleton
public class GndDataRepository {

  private static final String TAG = GndDataRepository.class.getSimpleName();

  private final DataService dataService;
  private Project oldActiveProject;
  private BehaviorSubject<ProjectActivationEvent> projectActivationObservable;
  private Flowable<DatastoreEvent<Feature>> placesObservable;

  @Inject
  public GndDataRepository(DataService dataService) {
    this.dataService = dataService;
    projectActivationObservable = BehaviorSubject.createDefault(
        ProjectActivationEvent.noProject());
  }

  public void onCreate() {
    dataService.onCreate();
  }

  public Observable<ProjectActivationEvent> activeProject() {
    return projectActivationObservable;
  }

  public CompletableFuture<Project> activateProject(String projectId) {
    projectActivationObservable.onNext(ProjectActivationEvent.loading());
    return dataService.loadProject(projectId).thenApply(project -> {
      oldActiveProject = project;
      projectActivationObservable.onNext(
          ProjectActivationEvent.activated(
              project,
              dataService.observePlaces(projectId),
              project.getFeatureTypesList()));
      return project;
    });
  }

  public Project getOldActiveProject() {
    return oldActiveProject;
  }

  public Feature update(FeatureUpdate featureUpdate) {
    return dataService.update(oldActiveProject.getId(), featureUpdate);
  }

  public Optional<FeatureType> getFeatureType(String featureTypeId) {
    return stream(oldActiveProject.getFeatureTypesList())
        .filter(ft -> ft.getId().equals(featureTypeId))
        .findFirst();
  }

  public CompletableFuture<List<Record>> getRecordData(String featureId) {
    return dataService.loadRecordData(oldActiveProject.getId(), featureId);
  }

  public CompletableFuture<List<Project>> getProjectSummaries() {
    return dataService.getProjectSummaries();
  }
}
