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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.OfflineBaseMapSelectorFragBinding;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.gnd.ui.offlinebasemap.selector.OfflineBaseMapSelectorViewModel.DownloadMessage;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import javax.inject.Inject;

@AndroidEntryPoint
public class OfflineBaseMapSelectorFragment extends AbstractFragment {

  private static final String MAP_FRAGMENT = MapProvider.class.getName() + "#fragment";

  @Inject Navigator navigator;
  @Inject MapProvider mapProvider;
  @Inject EphemeralPopups popups;

  private OfflineBaseMapSelectorViewModel viewModel;
  @Nullable private MapAdapter map;

  public static OfflineBaseMapSelectorFragment newInstance() {
    return new OfflineBaseMapSelectorFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(OfflineBaseMapSelectorViewModel.class);
    Single<MapAdapter> mapAdapter = mapProvider.getMapAdapter();
    mapAdapter.as(autoDisposable(this)).subscribe(this::onMapReady);
    viewModel
        .getDownloadMessages()
        // Since we pop a toast, we need to observe on the main (UI) thread.
        // Otherwise this subscription handler will trigger before Looper.prepare() has been called.
        .observeOn(AndroidSchedulers.mainThread())
        .as(autoDisposable(this))
        .subscribe(this::onDownloadMessage);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineBaseMapSelectorFragBinding binding =
        OfflineBaseMapSelectorFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    binding.downloadButton.setOnClickListener(__ -> onDownloadClick());
    ((MainActivity) getActivity()).setActionBar(binding.offlineAreaSelectorToolbar, true);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (savedInstanceState == null) {
      replaceFragment(R.id.map, mapProvider.getFragment());
    } else {
      mapProvider.restore(restoreChildFragment(savedInstanceState, MAP_FRAGMENT));
    }
  }

  /** Prepare the map once it's ready. */
  private void onMapReady(MapAdapter map) {
    this.map = map;
  }

  /** Handle a download button click and queue a basemap download. */
  public void onDownloadClick() {
    if (map == null) {
      return;
    }

    viewModel.downloadBaseMap(map.getViewport());
  }

  /** Handle the download message response after attempting to download a basemap. */
  private void onDownloadMessage(DownloadMessage message) {
    switch (message) {
      case STARTED:
        popups.showSuccess(R.string.offline_base_map_download_started);
        break;
      case FAILURE:
      default:
        popups.showError(R.string.offline_base_map_download_failed);
        break;
    }
    navigator.navigateUp();
  }
}
