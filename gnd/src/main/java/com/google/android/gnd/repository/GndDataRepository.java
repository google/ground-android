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

import static java8.util.stream.StreamSupport.stream;

import android.annotation.SuppressLint;
import com.google.android.gnd.service.DataService;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import com.google.android.gnd.vo.PlaceUpdate;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GndDataRepository {

  private static final String TAG = GndDataRepository.class.getSimpleName();

  private final DataService dataService;
  private BehaviorSubject<ProjectActivationEvent> projectActivationObservable;

  @Inject
  public GndDataRepository(DataService dataService) {
    this.dataService = dataService;
    projectActivationObservable = BehaviorSubject.createDefault(ProjectActivationEvent.noProject());
  }

  public void onCreate() {
    dataService.onCreate();
  }

  public Flowable<ProjectActivationEvent> activeProject() {
    return projectActivationObservable.toFlowable(BackpressureStrategy.LATEST);
  }

  @SuppressLint("CheckResult")
  public void activateProject(String projectId) {
    projectActivationObservable.onNext(ProjectActivationEvent.loading());
    dataService
      .loadProject(projectId)
      .subscribe(
        project ->
          projectActivationObservable.onNext(
            ProjectActivationEvent.activated(
              project,
              dataService.observePlaces(projectId),
              project.getPlaceTypes())));
  }

  public Place update(PlaceUpdate placeUpdate) {
    Project activeProject = projectActivationObservable.getValue().getProject();
    return dataService.update(activeProject.getId(), placeUpdate);
  }

  public Optional<PlaceType> getPlaceType(String placeTypeId) {
    Project activeProject = projectActivationObservable.getValue().getProject();
    return stream(activeProject.getPlaceTypes())
      .filter(pt -> pt.getId().equals(placeTypeId))
      .findFirst();
  }

  public Single<List<Record>> getRecordData(String placeId) {
    Project activeProject = projectActivationObservable.getValue().getProject();
    return dataService.loadRecordData(activeProject.getId(), placeId);
  }

  public Single<List<Project>> getProjectSummaries() {
    return dataService.fetchProjectSummaries();
  }
}
