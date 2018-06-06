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
import com.google.android.gnd.repository.Form;
import com.google.android.gnd.repository.Place;
import com.google.android.gnd.repository.PlaceType;
import com.google.android.gnd.ui.PlaceSheetEvent;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class FormTypePagerAdapter extends FragmentPagerAdapter {
  private List<Form> forms;
  private Place place;
  private PlaceType placeType;

  @Inject
  public FormTypePagerAdapter(FragmentManager fm) {
    super(fm);
    this.forms = Collections.emptyList();
  }

  @Override
  public int getCount() {
    return forms.size();
  }

  @Override
  public Fragment getItem(int position) {
    return RecordListFragment.newInstance(
      place.getPlaceTypeId(), place.getId(), forms.get(position).getId());
  }

  @Override
  public CharSequence getPageTitle(int position) {
    // TODO: i18n.
    return forms.get(position).getTitleOrDefault("pt", "Form " + position);
  }

  public void onPlaceSheetEvent(PlaceSheetEvent event) {
    if (event.isShowEvent()) {
      this.place = event.getPlace();
      this.placeType = event.getPlaceType();
      this.forms = event.getPlaceType().getFormsList();
      notifyDataSetChanged();
    }
  }
}
