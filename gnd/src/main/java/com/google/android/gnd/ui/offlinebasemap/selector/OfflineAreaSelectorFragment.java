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

package com.google.android.gnd.ui.offlinebasemap.selector;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.OfflineBaseMapSelectorFragBinding;
import com.google.android.gnd.model.basemap.tile.TileSet;
import com.google.android.gnd.ui.common.AbstractMapViewerFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.map.MapFragment;
import com.google.android.gnd.ui.offlinebasemap.selector.OfflineAreaSelectorViewModel.DownloadMessage;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class OfflineAreaSelectorFragment extends AbstractMapViewerFragment {

  @Inject Navigator navigator;
  @Inject EphemeralPopups popups;

  private OfflineAreaSelectorViewModel viewModel;

  public static OfflineAreaSelectorFragment newInstance() {
    return new OfflineAreaSelectorFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(OfflineAreaSelectorViewModel.class);
    viewModel.getDownloadMessages().observe(this, e -> e.ifUnhandled(this::onDownloadMessage));
  }

  private void onDownloadMessage(DownloadMessage message) {
    switch (message) {
      case STARTED:
        popups.showSuccess(R.string.offline_base_map_download_started);
        navigator.navigateUp();
        break;
      case FAILURE:
      default:
        popups.showError(R.string.offline_base_map_download_failed);
        navigator.navigateUp();
        break;
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineBaseMapSelectorFragBinding binding =
        OfflineBaseMapSelectorFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    ((MainActivity) getActivity()).setActionBar(binding.offlineAreaSelectorToolbar, true);
    return binding.getRoot();
  }

  @Override
  protected void onMapReady(MapFragment map) {
    viewModel
        .getRemoteTileSets()
        .map(tileSets -> stream(tileSets).map(TileSet::getUrl).collect(toImmutableList()))
        .as(autoDisposable(this))
        .subscribe(map::addRemoteTileOverlays);

    viewModel.requestRemoteTileSets();

    map.getCameraMovedEvents()
        .map(__ -> map.getViewport())
        .startWith(map.getViewport())
        .as(autoDisposable(this))
        .subscribe(viewModel::setViewport);
  }
}
