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

import com.google.android.gnd.service.DatastoreEvent;

import java.util.Collection;
import java.util.Map;

import io.reactivex.Flowable;
import java8.util.Optional;
import java8.util.stream.Collectors;

public class ProjectActivationEvent {

  public enum Status {
    NO_PROJECT,
    LOADING,
    ACTIVATED
  }

  private Project project;
  private Map<String, PlaceType> placeTypes;
  private Flowable<DatastoreEvent<Place>> placesFlowable;
  private Status status;

  private ProjectActivationEvent(Status status) {
    this.status = status;
  }

  public static ProjectActivationEvent noProject() {
    return new ProjectActivationEvent(Status.NO_PROJECT);
  }

  public static ProjectActivationEvent loading() {
    return new ProjectActivationEvent(Status.LOADING);
  }

  public static ProjectActivationEvent activated(
      Project project,
      Flowable<DatastoreEvent<Place>> placesObservable,
      Collection<PlaceType> placeTypes) {
    ProjectActivationEvent ev = new ProjectActivationEvent(Status.ACTIVATED);
    ev.project = project;
    ev.placesFlowable = placesObservable;
    ev.placeTypes = stream(placeTypes).collect(Collectors.toMap(PlaceType::getId, ft -> ft));
    return ev;
  }

  public Status getStatus() {
    return status;
  }

  public Project getProject() {
    return project;
  }

  public Flowable<DatastoreEvent<Place>> getPlacesFlowable() {
    return placesFlowable;
  }

  public Optional<PlaceType> getPlaceType(String placeTypeId) {
    return Optional.ofNullable(placeTypes.get(placeTypeId));
  }

  public boolean isActivated() {
    return status.equals(ProjectActivationEvent.Status.ACTIVATED);
  }
}
