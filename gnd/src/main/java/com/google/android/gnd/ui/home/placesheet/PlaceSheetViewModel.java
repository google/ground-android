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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.v4.view.ViewPager;
import com.google.android.gnd.ui.common.ActivityScope;
import com.google.android.gnd.ui.home.PlaceSheetState;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import java8.util.Optional;
import javax.inject.Inject;

@ActivityScope
public class PlaceSheetViewModel extends ViewModel implements ViewPager.OnPageChangeListener {

  private MutableLiveData<Optional<Place>> selectedPlace;
  private MutableLiveData<Optional<Form>> selectedForm;

  @Inject
  public PlaceSheetViewModel() {
    this.selectedPlace = new MutableLiveData<>();
    this.selectedForm = new MutableLiveData<>();
    selectedPlace.setValue(Optional.empty());
    selectedForm.setValue(Optional.empty());
  }

  public LiveData<Optional<Place>> getSelectedPlace() {
    return selectedPlace;
  }

  public LiveData<Optional<Form>> getSelectedForm() {
    return selectedForm;
  }

  @Override
  public void onPageSelected(int position) {
    selectedForm.setValue(
        selectedPlace
            .getValue()
            .map(Place::getPlaceType)
            .map(PlaceType::getForms)
            .filter(f -> f.size() > position)
            .map(f -> f.get(position)));
  }

  public void onPlaceSheetStateChange(PlaceSheetState state) {
    if (state.isVisible()) {
      selectedPlace.setValue(Optional.of(state.getPlace()));
      onPageSelected(0);
    } else {
      selectedPlace.setValue(Optional.empty());
      selectedForm.setValue(Optional.empty());
    }
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

  @Override
  public void onPageScrollStateChanged(int state) {}
}
