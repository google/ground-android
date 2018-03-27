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

package com.google.gnd.model;

import com.google.gnd.service.DataService;
import com.google.gnd.service.DataService.DataChangeListener;

import java.util.List;

import java8.util.Optional;
import java8.util.concurrent.CompletableFuture;

import static java8.util.stream.StreamSupport.stream;

public class DataModel {
  private final DataService dataService;
  private Project activeProject;

  public DataModel(DataService dataService) {
    this.dataService = dataService;
  }

  public void onCreate() {
    dataService.onCreate();
  }

  public CompletableFuture<Project> activateProject(String projectId) {
    dataService.removeAllListeners();
    return dataService
        .loadProject(projectId)
        .thenApply(
            project -> {
              activeProject = project;
              dataService.listenForFeatureChanges(projectId);
              return project;
            });
  }

  public void addFeatureChangeListener(DataChangeListener<Feature> listener) {
    dataService.addFeatureChangeListener(listener);
  }

  public Project getActiveProject() {
    return activeProject;
  }

  public Feature update(FeatureUpdate featureUpdate) {
    return dataService.update(activeProject.getId(), featureUpdate);
  }

  public Optional<FeatureType> getFeatureType(String featureTypeId) {
    return stream(activeProject.getFeatureTypesList())
        .filter(ft -> ft.getId().equals(featureTypeId))
        .findFirst();
  }

  public CompletableFuture<List<Record>> getRecordData(String featureId) {
    return dataService.loadRecordData(activeProject.getId(), featureId);
  }

  public CompletableFuture<List<Project>> getProjectSummaries() {
    return dataService.getProjectSummaries();
  }
}
