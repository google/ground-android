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

import com.google.android.gnd.service.DatastoreEvent;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Project;
import io.reactivex.Flowable;
import java8.util.Optional;

public class ProjectState {

  public enum Status {
    INACTIVE,
    LOADING,
    ACTIVATED
  }

  private Status status;
  private Optional<Project> project;
  private Flowable<DatastoreEvent<Place>> placesFlowable;

  private ProjectState(Status status) {
    this.status = status;
    this.project = Optional.empty();
  }

  public static ProjectState inactive() {
    return new ProjectState(Status.INACTIVE);
  }

  public static ProjectState loading() {
    return new ProjectState(Status.LOADING);
  }

  public static ProjectState activated(
    Project project,
    Flowable<DatastoreEvent<Place>> placesObservable) {
    ProjectState ev = new ProjectState(Status.ACTIVATED);
    ev.project = Optional.of(project);
    ev.placesFlowable = placesObservable;
    return ev;
  }

  public boolean isLoading() {
    return Status.LOADING.equals(status);
  }

  public Optional<Project> getActiveProject() {
    return project;
  }

  public Flowable<DatastoreEvent<Place>> getPlaces() {
    return placesFlowable;
  }

  public boolean isActivated() {
    return status.equals(ProjectState.Status.ACTIVATED);
  }
}
