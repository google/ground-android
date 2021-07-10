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

package com.google.android.gnd.ui.home.featuredetails;

import android.graphics.Bitmap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.MarkerIconFactory;
import com.google.android.gnd.ui.common.FeatureHelper;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.home.BottomSheetState;
import com.google.android.gnd.ui.util.DrawableUtil;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java8.util.Optional;
import javax.inject.Inject;

@SharedViewModel
public class FeatureDetailsViewModel extends ViewModel {

  @Hot
  private final FlowableProcessor<Optional<Feature>> featureFlowable =
      BehaviorProcessor.createDefault(Optional.empty());

  private final Bitmap markerBitmap;
  private final LiveData<String> title;
  private final LiveData<String> subtitle;

  private Optional<Feature> selectedFeature = Optional.empty();

  @Inject
  public FeatureDetailsViewModel(
      MarkerIconFactory markerIconFactory, DrawableUtil drawableUtil, FeatureHelper featureHelper) {
    this.markerBitmap =
        markerIconFactory.getMarkerBitmap(drawableUtil.getColor(R.color.colorGrey600));
    this.title =
        LiveDataReactiveStreams.fromPublisher(featureFlowable.map(featureHelper::getLabel));
    this.subtitle =
        LiveDataReactiveStreams.fromPublisher(featureFlowable.map(featureHelper::getSubtitle));
  }

  /**
   * Returns a LiveData that immediately emits the selected feature (or empty) on if none selected
   * to each new observer.
   */
  public LiveData<Optional<Feature>> getSelectedFeatureOnceAndStream() {
    return LiveDataReactiveStreams.fromPublisher(featureFlowable);
  }

  public boolean isSelectedFeatureOfTypePoint() {
    return selectedFeature.map(Feature::isPoint).orElse(true);
  }

  public void onBottomSheetStateChange(BottomSheetState state) {
    selectedFeature = !state.isVisible() ? Optional.empty() : state.getFeature();
    featureFlowable.onNext(selectedFeature);
  }

  public Bitmap getMarkerBitmap() {
    return markerBitmap;
  }

  public LiveData<String> getTitle() {
    return title;
  }

  public LiveData<String> getSubtitle() {
    return subtitle;
  }
}
