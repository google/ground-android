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

package com.google.android.gnd.ui.home.placesheet;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper;
import javax.inject.Inject;

public class PlaceSheetBodyFragment extends AbstractFragment {
  @Inject FormTabPagerAdapter formTypePagerAdapter;

  @BindView(R.id.forms_tab_layout)
  TabLayout formsTabLayout;

  @BindView(R.id.record_list_view_pager)
  ViewPager recordListViewPager;

  private PlaceSheetBodyViewModel viewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MainViewModel mainViewModel;

  @Inject
  public PlaceSheetBodyFragment() {}

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = get(PlaceSheetBodyViewModel.class);
    mainViewModel = get(MainViewModel.class);
    homeScreenViewModel = get(HomeScreenViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.place_sheet_frag, container, false);
  }

  @Override
  public void onViewCreated(
    @NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    recordListViewPager.setAdapter(formTypePagerAdapter);
    recordListViewPager.addOnPageChangeListener(viewModel);
    formsTabLayout.setupWithViewPager(recordListViewPager);
    TabLayoutHelper tabLayoutHelper = new TabLayoutHelper(formsTabLayout, recordListViewPager);
    // Stretch tabs if they all fit on screen, otherwise scroll.
    tabLayoutHelper.setAutoAdjustTabModeEnabled(true);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mainViewModel.getWindowInsets().observe(this, this::onApplyWindowInsets);
    homeScreenViewModel.getPlaceSheetState().observe(this, viewModel::onPlaceSheetStateChange);
    viewModel.getSelectedPlace().observe(this, formTypePagerAdapter::update);
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    recordListViewPager.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
  }
}
