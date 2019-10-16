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
import androidx.viewpager.widget.ViewPager;

import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.FeatureType;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.home.FeatureSheetState;

import javax.inject.Inject;

import java8.util.Optional;

@SharedViewModel
public class FeatureSheetViewModel extends ViewModel implements ViewPager.OnPageChangeListener {

  public final ObservableField<String> featureTitle = new ObservableField<>("hi");
  public final ObservableField<String> featureSubtitle = new ObservableField<>("hii");
  public final ObservableInt featureSubtitleVisibility = new ObservableInt();

  private final MutableLiveData<Optional<Feature>> selectedFeature;
  private final MutableLiveData<Optional<Form>> selectedForm;

  @Inject
  public FeatureSheetViewModel() {
    selectedFeature = new MutableLiveData<>();
    selectedForm = new MutableLiveData<>();
    selectedFeature.setValue(Optional.empty());
    selectedForm.setValue(Optional.empty());
  }

  public LiveData<Optional<Feature>> getSelectedFeature() {
    return selectedFeature;
  }

  public LiveData<Optional<Form>> getSelectedForm() {
    return selectedForm;
  }

  @Override
  public void onPageSelected(int position) {
    selectedForm.setValue(
            selectedFeature
                    .getValue()
                    .map(Feature::getFeatureType)
                    .map(FeatureType::getForms)
                    .filter(f -> f.size() > position)
                    .map(f -> f.get(position)));
  }

  public void onFeatureSheetStateChange(FeatureSheetState state) {
    if (state.isVisible()) {
      featureTitle.set(state.getFeature().getTitle());
      featureSubtitle.set(state.getFeature().getSubtitle());
      featureSubtitleVisibility.set(state.getFeature().getSubtitle().isEmpty() ? View.GONE : View.VISIBLE);

      selectedFeature.setValue(Optional.of(state.getFeature()));
      onPageSelected(0);
    } else {
      selectedFeature.setValue(Optional.empty());
      selectedForm.setValue(Optional.empty());
    }
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
  }

  @Override
  public void onPageScrollStateChanged(int state) {
  }
}
