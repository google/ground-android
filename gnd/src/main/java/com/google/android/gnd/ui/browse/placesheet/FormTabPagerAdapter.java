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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import javax.inject.Inject;

public class FormTabPagerAdapter extends FragmentStatePagerAdapter {
  private Optional<Place> place;

  @Inject
  public FormTabPagerAdapter(FragmentManager fm) {
    super(fm);
    place = Optional.empty();
  }

  @Override
  public int getCount() {
    return place
        .map(Place::getPlaceType)
        .map(PlaceType::getForms)
        .map(ImmutableList::size)
        .orElse(0);
  }

  @Override
  public Fragment getItem(int position) {
    return RecordListFragment.newInstance();
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return place.get().getPlaceType().getForms().get(position).getTitle();
  }

  void update(Optional<Place> place) {
    this.place = place;
    notifyDataSetChanged();
  }
}
