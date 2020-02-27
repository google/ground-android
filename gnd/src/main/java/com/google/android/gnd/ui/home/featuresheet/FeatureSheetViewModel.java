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

package com.google.android.gnd.ui.home.featuresheet;

import android.view.View;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.home.FeatureSheetState;
import java8.util.Optional;
import javax.inject.Inject;

@SharedViewModel
public class FeatureSheetViewModel extends ViewModel {

  public final ObservableField<String> featureTitle;
  public final ObservableField<String> featureSubtitle;
  public final ObservableInt featureSubtitleVisibility;

  private final MutableLiveData<Optional<Feature>> selectedFeature;

  @Inject
  public FeatureSheetViewModel() {
    featureTitle = new ObservableField<>();
    featureSubtitle = new ObservableField<>();
    featureSubtitleVisibility = new ObservableInt();
    selectedFeature = new MutableLiveData<>(Optional.empty());
  }

  public LiveData<Optional<Feature>> getSelectedFeature() {
    return selectedFeature;
  }

  public void onFeatureSheetStateChange(FeatureSheetState state) {
    if (!state.isVisible()) {
      selectedFeature.setValue(Optional.empty());
      return;
    }

    featureTitle.set(state.getFeature().getTitle());
    featureSubtitle.set(state.getFeature().getSubtitle());
    featureSubtitleVisibility.set(
        state.getFeature().getSubtitle().isEmpty() ? View.GONE : View.VISIBLE);

    selectedFeature.setValue(Optional.of(state.getFeature()));
  }
}
