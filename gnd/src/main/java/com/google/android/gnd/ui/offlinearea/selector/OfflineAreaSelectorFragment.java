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

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.OfflineAreaSelectorFragBinding;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.material.chip.Chip;
import io.reactivex.Single;
import javax.inject.Inject;

public class OfflineAreaSelectorFragment extends AbstractFragment {

  private static final String MAP_FRAGMENT = MapProvider.class.getName() + "#fragment";

  @Inject MapProvider mapProvider;

  @BindView(R.id.offline_area_selector_toolbar)
  TwoLineToolbar toolbar;

  // TODO: Use data binding
  @BindView(R.id.download_button)
  Chip downloadButton;

  private OfflineAreaSelectorViewModel viewModel;
  @Nullable
  private MapAdapter map;

  public static OfflineAreaSelectorFragment newInstance() {
    return new OfflineAreaSelectorFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getViewModel(MainViewModel.class).getWindowInsets().observe(this, this::onApplyWindowInsets);

    viewModel = getViewModel(OfflineAreaSelectorViewModel.class);
    // TODO: use the viewmodel
    Single<MapAdapter> mapAdapter = mapProvider.getMapAdapter();
    mapAdapter.as(autoDisposable(this)).subscribe(this::onMapReady);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineAreaSelectorFragBinding binding =
        OfflineAreaSelectorFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    ((MainActivity) getActivity()).setActionBar(binding.offlineAreaSelectorToolbar, true);
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

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    toolbar.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
    downloadButton.setTranslationY(-insets.getSystemWindowInsetBottom());
    ViewCompat.onApplyWindowInsets(mapProvider.getFragment().getView(), insets);
  }

  private void onMapReady(MapAdapter map) {
    this.map = map;
  }

  @OnClick(R.id.download_button)
  public void onDownloadClick() {
    if (map == null) {
      return;
    }

    viewModel.onDownloadClick(map.getViewport());
  }
}
