/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.offlinearea;

import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.repository.OfflineAreaRepository;
import com.google.android.gnd.system.GeocodingManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.common.collect.ImmutableMap;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java8.util.stream.Collectors;
import javax.inject.Inject;

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
public class OfflineAreasViewModel extends AbstractViewModel {

  private LiveData<ImmutableMap<String, OfflineArea>> offlineAreas;
  private final Navigator navigator;
  private final GeocodingManager geocoder;

  @Inject
  OfflineAreasViewModel(
      Navigator navigator,
      OfflineAreaRepository offlineAreaRepository,
      @ApplicationContext Context context) {
    this.navigator = navigator;
    this.geocoder = new GeocodingManager(context);
    this.offlineAreas =
        LiveDataReactiveStreams.fromPublisher(
            offlineAreaRepository
                .getOfflineAreasOnceAndStream()
                .map(
                    areas ->
                        stream(areas)
                            .collect(Collectors.toMap(geocoder::getOfflineAreaName, this::id)))
                .map(ImmutableMap::copyOf));
  }

  public void showOfflineAreaSelector() {
    navigator.showOfflineAreaSelector();
  }

  private OfflineArea id(OfflineArea area) {
    return area;
  }

  LiveData<ImmutableMap<String, OfflineArea>> getOfflineAreas() {
    return offlineAreas;
  }
}
