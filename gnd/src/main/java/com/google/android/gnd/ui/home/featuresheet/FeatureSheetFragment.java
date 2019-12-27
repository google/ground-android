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

package com.google.android.gnd.ui.home.featuresheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.FeatureSheetFragBinding;
import com.google.android.gnd.ui.MapIcon;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.FeatureSheetState;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.material.tabs.TabLayout;
import javax.inject.Inject;

public class FeatureSheetFragment extends AbstractFragment {

  @Inject FormTabPagerAdapter formTypePagerAdapter;

  @BindView(R.id.feature_header_icon)
  ImageView featureHeaderIcon;

  @BindView(R.id.forms_tab_layout)
  TabLayout formsTabLayout;

  @BindView(R.id.observation_list_view_pager)
  ViewPager observationListViewPager;

  private FeatureSheetViewModel viewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MainViewModel mainViewModel;

  @Inject
  public FeatureSheetFragment() {}

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(FeatureSheetViewModel.class);
    mainViewModel = getViewModel(MainViewModel.class);
    homeScreenViewModel = getViewModel(HomeScreenViewModel.class);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FeatureSheetFragBinding binding = FeatureSheetFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    observationListViewPager.setAdapter(formTypePagerAdapter);
    observationListViewPager.addOnPageChangeListener(viewModel);
    formsTabLayout.setupWithViewPager(observationListViewPager);
    // TODO: See if this is still needed; not compatible with latest v4 support libs.
    // Stretch tabs if they all fit on screen, otherwise scroll.
    // TabLayoutHelper tabLayoutHelper = new TabLayoutHelper(formsTabLayout,
    // observationListViewPager);
    // tabLayoutHelper.setAutoAdjustTabModeEnabled(true);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mainViewModel.getWindowInsets().observe(this, this::onApplyWindowInsets);
    homeScreenViewModel.getFeatureSheetState().observe(this, this::onFeatureSheetStateChange);
    viewModel.getSelectedFeature().observe(this, formTypePagerAdapter::update);
  }

  private void onFeatureSheetStateChange(FeatureSheetState featureSheetState) {
    if (featureSheetState.isVisible()) {
      String iconOverlayId = null; // Not yet implemented.
      featureHeaderIcon.setImageResource(MapIcon.getResourceId(getContext(), iconOverlayId));

      // TODO: Auto add observation if there's only one form.
      //      Feature feature = featureSheetState.getFeature();
      //      ImmutableList<Form> forms = feature.getLayer().getForms();
      //      if (featureSheetState.isNewFeature() && forms.size() == 1) {
      //        showAddObservation(feature, forms.get(0));
      //      }
    }

    viewModel.onFeatureSheetStateChange(featureSheetState);
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    observationListViewPager.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
  }
}
