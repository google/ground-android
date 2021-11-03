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

import android.content.res.Resources;
import com.google.android.gnd.R;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.GeoJsonFeature;
import java8.util.Optional;
import javax.inject.Inject;
import org.jetbrains.annotations.Nullable;

/** Common logic for formatting attributes of {@link Feature} for display to the user. */
public class FeatureHelper {

  private final Resources resources;

  @Inject
  FeatureHelper(Resources resources) {
    this.resources = resources;
  }

  public String getCreatedBy(Optional<Feature> feature) {
    return getUserName(feature)
        .map(name -> resources.getString(R.string.added_by, name))
        .orElse("");
  }

  public String getLabel(Optional<Feature> feature) {
    return getCaption(feature).orElseGet(() -> feature.map(this::getFeatureType).orElse(""));
  }

  private String getFeatureType(Feature feature) {
    if (feature.isGeoJson() || feature.isPolygon()) {
      return resources.getString(R.string.polygon);
    } else {
      return resources.getString(R.string.point);
    }
  }

  public String getSubtitle(Optional<Feature> feature) {
    return feature
        .map(f -> resources.getString(R.string.layer_label_format, f.getLayer().getName()))
        .orElse("");
  }

  private Optional<String> getUserName(Optional<Feature> feature) {
    return feature.map(Feature::getCreated).map(AuditInfo::getUser).map(User::getDisplayName);
  }

  private Optional<String> getCaption(Optional<Feature> feature) {
    // TODO(#793): Allow user-defined feature names for other feature types.
    return feature.map(this::getCaption).map(String::trim).filter(caption -> !caption.isEmpty());
  }

  @Nullable
  private String getCaption(Feature feature) {
    if (feature.isGeoJson()) {
      return getGeoJsonCaption((GeoJsonFeature) feature);
    }
    return feature.getCaption();
  }

  private String getGeoJsonCaption(GeoJsonFeature feature) {
    String caption = feature.getCaptionFromProperties();
    return caption.isEmpty()
        ? getFeatureType(feature) + " " + feature.getIdFromProperties()
        : caption;
  }
}
