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

package com.google.android.gnd.ui.browse.placesheet;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.browse.BrowseViewModel;
import com.google.android.gnd.ui.common.GndFragment;
import com.google.android.gnd.ui.common.GndViewModelFactory;
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper;
import javax.inject.Inject;

public class PlaceSheetBodyFragment extends GndFragment {
  @Inject GndViewModelFactory viewModelFactory;

  @Inject FormTypePagerAdapter formTypePagerAdapter;

  @BindView(R.id.record_list_view_pager)
  ViewPager recordListViewPager;

  @BindView(R.id.forms_tab_layout)
  TabLayout formsTabLayout;

  private BrowseViewModel browseViewModel;
  private MainViewModel mainViewModel;

  @Inject
  public PlaceSheetBodyFragment() {}

  @Override
  protected void createViewModel() {
    mainViewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(MainViewModel.class);
    browseViewModel =
        ViewModelProviders.of(getParentFragment(), viewModelFactory).get(BrowseViewModel.class);
  }

  @Override
  protected View createView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_place_sheet, container, false);
  }

  @Override
  protected void initializeViews() {
    recordListViewPager.setAdapter(formTypePagerAdapter);
    formsTabLayout.setupWithViewPager(recordListViewPager);
    TabLayoutHelper tabLayoutHelper = new TabLayoutHelper(formsTabLayout, recordListViewPager);
    // Stretch tabs if they all fit on screen, otherwise scroll.
    tabLayoutHelper.setAutoAdjustTabModeEnabled(true);
  }

  @Override
  protected void observeViewModel() {
    mainViewModel.getWindowInsetsLiveData().observe(this, this::onApplyWindowInsets);
    browseViewModel.getPlaceSheetEvents().observe(this, formTypePagerAdapter::onPlaceSheetEvent);
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    recordListViewPager.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
  }
}
