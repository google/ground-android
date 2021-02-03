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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.MarkerIconFactory;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.home.BottomSheetState;
import com.google.android.gnd.ui.util.DrawableUtil;
import io.reactivex.processors.BehaviorProcessor;
import java8.util.Optional;
import javax.inject.Inject;

@SharedViewModel
public class FeatureDetailsViewModel extends ViewModel {

  @Hot(replays = true)
  public final MutableLiveData<Optional<Feature>> feature = new MutableLiveData<>();

  @Hot(replays = true)
  private final BehaviorProcessor<Optional<Feature>> selectedFeature =
      BehaviorProcessor.createDefault(Optional.empty());

  private final Bitmap markerBitmap;

  @Inject
  public FeatureDetailsViewModel(MarkerIconFactory markerIconFactory, DrawableUtil drawableUtil) {
    this.markerBitmap =
        markerIconFactory.getMarkerBitmap(drawableUtil.getColor(R.color.colorGrey600));
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

    Optional<Feature> featureOptional = state.getFeature();
    feature.setValue(featureOptional);
    selectedFeature.onNext(featureOptional);
  }

  public Bitmap getMarkerBitmap() {
    return markerBitmap;
  }
}
