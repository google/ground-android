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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.layer.Layer;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import javax.inject.Inject;

// TODO: Delete me.
public class FormTabPagerAdapter extends FragmentStatePagerAdapter {
  private Optional<Feature> feature;

  @Inject
  public FormTabPagerAdapter(FragmentManager fm) {
    super(fm);
    feature = Optional.empty();
  }

  @Override
  public int getCount() {
    return feature.map(Feature::getLayer).map(Layer::getForms).map(ImmutableList::size).orElse(0);
  }

  @Override
  public Fragment getItem(int position) {
    return ObservationListFragment.newInstance();
  }

  @Override
  public CharSequence getPageTitle(int position) {

    return "";
  }

  void update(Optional<Feature> feature) {
    this.feature = feature;
    notifyDataSetChanged();
  }
}
