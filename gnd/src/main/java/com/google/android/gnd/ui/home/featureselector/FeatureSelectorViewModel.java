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
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import javax.inject.Inject;

public class FeatureSelectorViewModel extends AbstractViewModel {
  private ImmutableList<Feature> features = ImmutableList.<Feature>builder().build();
  private final PublishSubject<Integer> selections = PublishSubject.create();
  private final Observable<Feature> selectedFeatures;

  @Inject
  FeatureSelectorViewModel() {
    this.selectedFeatures = selections.map(i -> this.features.get(i));
  }

  public void onFeatures(ImmutableList<Feature> features) {
    this.features = features;
  }

  public void selectFeature(int index) {
    selections.onNext(index);
  }

  public Observable<ImmutableList<Feature>> getFeatures() {
    return Single.just(features).toObservable();
  }

  public Observable<Feature> getFeatureSelections() {
    return selectedFeatures;
  }

  String getListItemText(Feature feature) {
    String text = "";
    if (feature.isGeoJson()) {
      text = "Area\n";
    } else if (feature.isPoint()) {
      text = "Point\n";
    }

    return text + feature.getLayer().getName();
  }
}
