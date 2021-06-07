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

package com.google.android.gnd.ui.home.featureselector;

import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

@SharedViewModel
public class FeatureSelectorViewModel extends AbstractViewModel {

  private ImmutableList<Feature> features = ImmutableList.<Feature>builder().build();
  @Hot private final Subject<Integer> itemClicks = PublishSubject.create();
  @Hot private final Observable<Feature> featureClicks;

  @Inject
  FeatureSelectorViewModel() {
    this.featureClicks = itemClicks.filter(i -> i < features.size()).map(i -> features.get(i));
  }

  public void setFeatures(ImmutableList<Feature> features) {
    this.features = features;
  }

  public void onItemClick(int index) {
    itemClicks.onNext(index);
  }

  public ImmutableList<Feature> getFeatures() {
    return features;
  }

  public Observable<Feature> getFeatureClicks() {
    return featureClicks;
  }
}
