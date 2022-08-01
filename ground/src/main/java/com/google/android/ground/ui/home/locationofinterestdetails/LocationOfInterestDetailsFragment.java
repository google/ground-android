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

package com.google.android.ground.ui.home.locationofinterestdetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.ground.MainViewModel;
import com.google.android.ground.R;
import com.google.android.ground.databinding.LocationOfInterestDetailsFragBinding;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.ui.common.AbstractFragment;
import com.google.android.ground.ui.home.BottomSheetState;
import com.google.android.ground.ui.home.HomeScreenViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.Optional;
import java8.util.stream.IntStreams;
import javax.inject.Inject;

/** Fragment containing the contents of the bottom sheet shown when a LOI is selected. */
@AndroidEntryPoint
public class LocationOfInterestDetailsFragment extends AbstractFragment {

  private LocationOfInterestDetailsViewModel viewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MainViewModel mainViewModel;
  private LocationOfInterestDetailsFragBinding binding;

  @Inject
  public LocationOfInterestDetailsFragment() {}

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(LocationOfInterestDetailsViewModel.class);
    mainViewModel = getViewModel(MainViewModel.class);
    homeScreenViewModel = getViewModel(HomeScreenViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = LocationOfInterestDetailsFragBinding.inflate(inflater, container, false);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
    mainViewModel.getWindowInsets().observe(getViewLifecycleOwner(), this::onApplyWindowInsets);
    homeScreenViewModel
        .getBottomSheetState()
        .observe(getViewLifecycleOwner(), this::onBottomSheetStateChange);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.loi_sheet_menu, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    IntStreams.range(0, menu.size() - 1)
        .boxed()
        .map(menu::getItem)
        .forEach(
            menuItem -> {
              if (menuItem.getItemId() == R.id.move_loi_menu_item) {
                viewModel.isMoveMenuOptionVisible().observe(this, menuItem::setVisible);
              }
            });
  }

  private void onBottomSheetStateChange(BottomSheetState state) {
    LocationOfInterest loi = state.isVisible() ? state.getLocationOfInterest() : null;
    viewModel.onLocationOfInterestSelected(Optional.ofNullable(loi));
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    binding
        .getRoot()
        .findViewById(R.id.submission_list_container)
        .setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
  }

  @Nullable
  @Override
  public View getView() {
    return super.getView();
  }
}
