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

package com.google.android.gnd.ui.placesheet;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.google.android.gnd.ui.PlaceSheetEvent;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import javax.inject.Inject;

public class FormTypePagerAdapter extends FragmentPagerAdapter {
  private Optional<Place> place;

  @Inject
  public FormTypePagerAdapter(FragmentManager fm) {
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
    PlaceType placeType = place.get().getPlaceType();
    Form form = placeType.getForms().get(position);
    return RecordListFragment.newInstance(
      placeType.getId(), place.get().getId(), form.getId());
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return place.get().getPlaceType().getForms().get(position).getTitle();
  }

  public void onPlaceSheetEvent(PlaceSheetEvent event) {
    if (event.isShowEvent()) {
      this.place = Optional.of(event.getPlace());
      notifyDataSetChanged();
    }
  }
}
