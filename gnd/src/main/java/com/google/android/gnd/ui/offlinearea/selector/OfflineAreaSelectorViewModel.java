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

package com.google.android.gnd.ui.offlinearea.selector;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.repository.OfflineAreaRepository;
import com.google.android.gnd.ui.common.AbstractViewModel;
import javax.inject.Inject;
import timber.log.Timber;

public class OfflineAreaSelectorViewModel extends AbstractViewModel {

  private final OfflineAreaRepository offlineAreaRepository;

  @Inject
  OfflineAreaSelectorViewModel(OfflineAreaRepository offlineAreaRepository) {
    this.offlineAreaRepository = offlineAreaRepository;
  }

  // TODO: Use an abstraction over LatLngBounds
  public void onDownloadClick(LatLngBounds viewport) {
    Timber.d("viewport:%s", viewport);
    offlineAreaRepository
        .addAreaAndEnqueue(viewport)
        .doOnError(err -> Timber.e("Failed to add area and queue downloads: %s", err.getMessage()))
        .onErrorComplete()
        .blockingAwait();
  }
}
