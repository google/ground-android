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

package com.google.android.gnd.ui.offlinebasemap;

import android.view.View;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.basemap.OfflineBaseMap;
import com.google.android.gnd.repository.OfflineBaseMapRepository;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
public class OfflineBaseMapsViewModel extends AbstractViewModel {

  private final LiveData<ImmutableList<OfflineBaseMap>> offlineAreas;
  private final LiveData<Integer> noAreasMessageVisibility;

  private final Navigator navigator;

  @Inject
  OfflineBaseMapsViewModel(Navigator navigator, OfflineBaseMapRepository offlineBaseMapRepository) {
    this.navigator = navigator;
    Flowable<ImmutableList<OfflineBaseMap>> offlineAreas =
        offlineBaseMapRepository
            .getOfflineAreasOnceAndStream()
            .doOnError(
                throwable ->
                    Timber.e(
                        throwable,
                        "Unexpected error accessing offline basemaps in the local store."))
            .onErrorReturnItem(ImmutableList.of());
    this.offlineAreas = LiveDataReactiveStreams.fromPublisher(offlineAreas);
    this.noAreasMessageVisibility =
        LiveDataReactiveStreams.fromPublisher(
            offlineAreas.map(baseMaps -> baseMaps.isEmpty() ? View.VISIBLE : View.GONE));
  }

  /** Navigate to the offline area selector UI from the offline basemaps UI. */
  public void showOfflineAreaSelector() {
    navigator.navigate(OfflineBaseMapsFragmentDirections.showOfflineAreaSelector());
  }

  /**
   * Returns the current list of downloaded offline basemaps available for viewing. If an unexpected
   * error accessing the local store is encountered, emits an empty list, circumventing the error.
   */
  LiveData<ImmutableList<OfflineBaseMap>> getOfflineAreas() {
    return offlineAreas;
  }

  /**
   * Returns the visibility of a "no area" message based on the current number of available offline
   * basemaps.
   */
  public LiveData<Integer> getNoAreasMessageVisibility() {
    return noAreasMessageVisibility;
  }
}
