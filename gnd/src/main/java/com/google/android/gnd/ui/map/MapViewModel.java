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

package com.google.android.gnd.ui.map;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.graphics.Color;
import com.google.android.gnd.model.GndDataRepository;
import com.google.android.gnd.model.Place;
import com.google.android.gnd.model.PlaceType;
import com.google.android.gnd.model.ProjectActivationEvent;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.service.DatastoreEvent;

public class MapViewModel extends ViewModel {
  private final LiveData<MarkerUpdate> markerUpdates;

  MapViewModel(GndDataRepository dataRepository) {
    markerUpdates = RxLiveData.fromObservable(
        dataRepository
            .activeProject()
            .filter(ProjectActivationEvent::isActivated)
            .switchMap(project ->
                project.getPlacesFlowable().toObservable()
                    .map(placeData -> toMarkerUpdate(project, placeData))
                    .filter(MarkerUpdate::isValid)
                    .startWith(MarkerUpdate.clearAll())));
  }

  public static MarkerUpdate toMarkerUpdate(ProjectActivationEvent project,
      DatastoreEvent<Place> placeData) {
    switch (placeData.getType()) {
      case ENTITY_LOADED:
      case ENTITY_MODIFIED:
        return placeData.getEntity()
            .map(Place::getPlaceTypeId)
            .flatMap(project::getPlaceType)
            .map(placeType ->
                MarkerUpdate.addOrUpdatePlace(
                    placeData.getEntity().get(), // TODO: Remove Place from MarkerUpdate.
                    placeType.getIconId(),
                    getIconColor(placeType),
                    hasPendingWrites(placeData)))
            .orElse(MarkerUpdate.invalid());
      case ENTITY_REMOVED:
        return MarkerUpdate.remove(placeData.getId());
    }
    return MarkerUpdate.invalid();
  }

  private static int getIconColor(PlaceType placeType) {
    // TODO: Return default color if invalid.
    return Color.parseColor(placeType.getIconColor());
  }

  private static boolean hasPendingWrites(DatastoreEvent<Place> placeDataEvent) {
    return placeDataEvent.getSource() == DatastoreEvent.Source.LOCAL_DATASTORE;
  }

  LiveData<MarkerUpdate> mapMarkers() {
    return markerUpdates;
  }
}
