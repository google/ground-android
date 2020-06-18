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

package com.google.android.gnd.ui.offlinearea.viewer;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.OnClick;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.OfflineAreaViewerFragBinding;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapProvider;
import io.reactivex.Single;
import javax.inject.Inject;

public class OfflineAreaViewerFragment extends AbstractFragment {

  private static final String MAP_FRAGMENT = MapProvider.class.getName() + "#fragment";

  @Inject MapProvider mapProvider;

  private OfflineAreaViewerViewModel viewModel;
  @Nullable private MapAdapter map;

  @Inject
  public OfflineAreaViewerFragment() {}

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    OfflineAreaViewerFragmentArgs args = OfflineAreaViewerFragmentArgs.fromBundle(getArguments());
    viewModel = getViewModel(OfflineAreaViewerViewModel.class);
    viewModel.loadOfflineArea(args);
    Single<MapAdapter> mapAdapter = mapProvider.getMapAdapter();
    mapAdapter.as(autoDisposable(this)).subscribe(this::onMapReady);
    viewModel.getOfflineArea().observe(this, this::panMap);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineAreaViewerFragBinding binding =
        OfflineAreaViewerFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    ((MainActivity) getActivity()).setActionBar(binding.offlineAreaViewerToolbar, true);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (savedInstanceState == null) {
      replaceFragment(R.id.map, mapProvider.getFragment());
    } else {
      mapProvider.restore(restoreChildFragment(savedInstanceState, MAP_FRAGMENT));
    }
  }

  private void onMapReady(MapAdapter map) {
    this.map = map;
  }

  private void panMap(OfflineArea offlineArea) {
    if (map == null) {
      return;
    }

    double lat = offlineArea.getBounds().northeast.latitude;
    double lon = offlineArea.getBounds().southwest.longitude;
    Point point = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();
    map.moveCamera(point);
  }

  @OnClick(R.id.remove_button)
  public void onRemoveClick() {
    if (map == null) {
      return;
    }

    viewModel.onRemoveClick();
  }
}
