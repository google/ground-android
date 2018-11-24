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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.MapIcon;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.FeatureSheetState;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import javax.inject.Inject;

public class FeatureSheetFragment extends AbstractFragment {
  @Inject FormTabPagerAdapter formTypePagerAdapter;

  @BindView(R.id.feature_sheet_title)
  TextView featureSheetTitle;

  @BindView(R.id.feature_sheet_subtitle)
  TextView featureSheetSubtitle;

  @BindView(R.id.feature_header_icon)
  ImageView featureHeaderIcon;

  @BindView(R.id.forms_tab_layout)
  TabLayout formsTabLayout;

  @BindView(R.id.record_list_view_pager)
  ViewPager recordListViewPager;

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
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.feature_sheet_frag, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    recordListViewPager.setAdapter(formTypePagerAdapter);
    recordListViewPager.addOnPageChangeListener(viewModel);
    formsTabLayout.setupWithViewPager(recordListViewPager);
    // TODO: See if this is still needed; not compatible with latest v4 support libs.
    // Stretch tabs if they all fit on screen, otherwise scroll.
    // TabLayoutHelper tabLayoutHelper = new TabLayoutHelper(formsTabLayout, recordListViewPager);
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
      featureHeaderIcon.setImageResource(
          MapIcon.getResourceId(
              getContext(), featureSheetState.getFeature().getFeatureType().getIconId()));
      featureSheetTitle.setText(featureSheetState.getFeature().getTitle());
      featureSheetSubtitle.setText(featureSheetState.getFeature().getSubtitle());
      featureSheetSubtitle.setVisibility(
          featureSheetState.getFeature().getSubtitle().isEmpty() ? View.GONE : View.VISIBLE);

      // TODO: Auto add record if there's only one form.
      //      Feature feature = featureSheetState.getFeature();
      //      ImmutableList<Form> forms = feature.getFeatureType().getForms();
      //      if (featureSheetState.isNewFeature() && forms.size() == 1) {
      //        showAddRecord(feature, forms.get(0));
      //      }
    }

    viewModel.onFeatureSheetStateChange(featureSheetState);
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    recordListViewPager.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
  }
}
