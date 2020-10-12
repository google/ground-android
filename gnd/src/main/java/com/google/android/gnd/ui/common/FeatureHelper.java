/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.common;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gnd.R;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.layer.Layer;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ActivityScoped;
import java8.util.Optional;
import javax.inject.Inject;

/** Common logic for formatting attributes of {@link Feature} for display to the user. */
@ActivityScoped
public class FeatureHelper {

  private final Context context;

  @Inject
  FeatureHelper(@ApplicationContext Context context) {
    this.context = context;
  }

  public String getCreatedBy(@NonNull Optional<Feature> feature) {
    return getUserName(feature).map(name -> context.getString(R.string.added_by, name)).orElse("");
  }

  public String getTitle(@NonNull Optional<Feature> feature) {
    return getCaption(feature).orElseGet(() -> getLayerName(feature).orElse(""));
  }

  private Optional<String> getUserName(@NonNull Optional<Feature> feature) {
    return feature.map(Feature::getCreated).map(AuditInfo::getUser).map(User::getDisplayName);
  }

  private Optional<String> getCaption(@NonNull Optional<Feature> feature) {
    return feature.map(Feature::getCaption).map(String::trim).filter(caption -> !caption.isEmpty());
  }

  private Optional<String> getLayerName(@NonNull Optional<Feature> feature) {
    return feature.map(Feature::getLayer).map(Layer::getName);
  }
}
