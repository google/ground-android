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

package com.google.android.gnd.ui.home.featuredetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import butterknife.BindView;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.FeatureDetailsFragBinding;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.BottomSheetState;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import javax.inject.Inject;

public class FeatureDetailsFragment extends AbstractFragment {

  @BindView(R.id.feature_header_icon)
  ImageView featureHeaderIcon;

  @BindView(R.id.observation_list_container)
  View observationListContainer;

  private FeatureDetailsViewModel viewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MainViewModel mainViewModel;

  @Inject
  public FeatureDetailsFragment() {}

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(FeatureDetailsViewModel.class);
    mainViewModel = getViewModel(MainViewModel.class);
    homeScreenViewModel = getViewModel(HomeScreenViewModel.class);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FeatureDetailsFragBinding binding =
        FeatureDetailsFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mainViewModel.getWindowInsets().observe(getViewLifecycleOwner(), this::onApplyWindowInsets);
    homeScreenViewModel
        .getBottomSheetState()
        .observe(getViewLifecycleOwner(), this::onBottomSheetStateChange);
  }

  private void onBottomSheetStateChange(BottomSheetState state) {
    if (state.isVisible()) {
      // TODO(#373): Update icon based on layer default style.
      // TODO: Auto add observation if there's only one form.
      //      Feature feature = state.getFeature();
      //      ImmutableList<Form> forms = feature.getLayer().getForms();
      //      if (state.isNewFeature() && forms.size() == 1) {
      //        showAddObservation(feature, forms.get(0));
      //      }
    }

    viewModel.onBottomSheetStateChange(state);
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    observationListContainer.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
  }
}
