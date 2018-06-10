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
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GndDataRepository {

  private static final String TAG = GndDataRepository.class.getSimpleName();

  private final DataService dataService;
  private BehaviorSubject<ProjectState> projectStateObservable;

  @Inject
  public GndDataRepository(DataService dataService) {
    this.dataService = dataService;
    projectStateObservable = BehaviorSubject.createDefault(ProjectState.inactive());
  }

  public void onCreate() {
    dataService.onCreate();
  }

  public Flowable<ProjectState> projectState() {
    return projectStateObservable.toFlowable(BackpressureStrategy.LATEST);
  }

  @SuppressLint("CheckResult")
  public void activateProject(String projectId) {
    projectStateObservable.onNext(ProjectState.loading());
    dataService
      .loadProject(projectId)
      .subscribe(
        project ->
          projectStateObservable.onNext(
            ProjectState.activated(
              project,
              dataService.observePlaces(projectId))));
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
