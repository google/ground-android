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

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.home.BottomSheetState;
import io.reactivex.processors.BehaviorProcessor;
import java8.util.Optional;
import javax.inject.Inject;

@SharedViewModel
public class FeatureDetailsViewModel extends ViewModel {

  public final ObservableField<String> featureTitle;
  public final ObservableField<String> featureSubtitle;
  public final ObservableBoolean isFeatureSubtitleVisible = new ObservableBoolean(false);

  private final BehaviorProcessor<Optional<Feature>> selectedFeature;

  @Inject
  public FeatureDetailsViewModel() {
    featureTitle = new ObservableField<>("My Title");
    featureSubtitle = new ObservableField<>();
    selectedFeature = BehaviorProcessor.createDefault(Optional.empty());
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

    featureTitle.set(state.getFeature().getTitle());
    featureSubtitle.set(state.getFeature().getSubtitle());
    isFeatureSubtitleVisible.set(!state.getFeature().getSubtitle().isEmpty());

    selectedFeature.onNext(Optional.of(state.getFeature()));
  }
}
