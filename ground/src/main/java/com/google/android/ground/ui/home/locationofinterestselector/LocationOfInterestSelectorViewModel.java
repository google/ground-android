/*
 * Copyright 2021 Google LLC
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

package com.google.android.ground.ui.home.locationofinterestselector;

import android.content.res.Resources;
import com.google.android.ground.R;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.ui.common.AbstractViewModel;
import com.google.android.ground.ui.common.LocationOfInterestHelper;
import com.google.android.ground.ui.common.SharedViewModel;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.ArrayList;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;

@SharedViewModel
public class LocationOfInterestSelectorViewModel extends AbstractViewModel {

  @Hot private final Subject<Integer> itemClicks = PublishSubject.create();
  @Hot private final Observable<LocationOfInterest> locationOfInterestClicks;
  private final LocationOfInterestHelper locationOfInterestHelper;
  private final Resources resources;
  private final List<LocationOfInterest> locationsOfInterest = new ArrayList<>();

  @Inject
  LocationOfInterestSelectorViewModel(
      LocationOfInterestHelper locationOfInterestHelper, Resources resources) {
    this.locationOfInterestClicks =
        itemClicks.filter(i -> i < locationsOfInterest.size()).map(locationsOfInterest::get);
    this.locationOfInterestHelper = locationOfInterestHelper;
    this.resources = resources;
  }

  public void onItemClick(int index) {
    itemClicks.onNext(index);
  }

  public List<LocationOfInterest> getLocationsOfInterest() {
    return locationsOfInterest;
  }

  public Observable<LocationOfInterest> getLocationOfInterestClicks() {
    return locationOfInterestClicks;
  }

  String getListItemText(LocationOfInterest locationOfInterest) {
    // TODO: Add icons and custom view layout for list items.
    return locationOfInterestHelper.getLabel(Optional.of(locationOfInterest))
        + "\n"
        + resources.getString(R.string.layer_label_format, locationOfInterest.getJob().getName());
  }
}
