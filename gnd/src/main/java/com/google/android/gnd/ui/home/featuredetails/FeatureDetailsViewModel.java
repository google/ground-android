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

  //  @Hot(replays = true)
  //  public final MutableLiveData<Optional<Feature>> feature = new MutableLiveData<>();

  //  @Hot(replays = true)

  @Hot
  private final FlowableProcessor<Optional<Feature>> selectedFeature =
      BehaviorProcessor.createDefault(Optional.empty());

  private final Bitmap markerBitmap;
  private final FeatureHelper featureHelper;
  private LiveData<String> title;
  private LiveData<String> subtitle;

  @Inject
  public FeatureDetailsViewModel(
      MarkerIconFactory markerIconFactory, DrawableUtil drawableUtil, FeatureHelper featureHelper) {
    this.markerBitmap =
        markerIconFactory.getMarkerBitmap(drawableUtil.getColor(R.color.colorGrey600));
    this.featureHelper = featureHelper;
    this.title =
        LiveDataReactiveStreams.fromPublisher(selectedFeature.map(featureHelper::getTitle));
    this.subtitle =
        LiveDataReactiveStreams.fromPublisher(selectedFeature.map(featureHelper::getSubtitle));
  }

  /**
   * Returns a LiveData that immediately emits the selected feature (or empty) on if none selected
   * to each new observer.
   */
  public LiveData<Optional<Feature>> getSelectedFeatureOnceAndStream() {
    return LiveDataReactiveStreams.fromPublisher(selectedFeature);
  }

  public void onBottomSheetStateChange(BottomSheetState state) {
    if (!state.isVisible()) {
      selectedFeature.onNext(Optional.empty());
      return;
    }

    selectedFeature.onNext(state.getFeature());
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
