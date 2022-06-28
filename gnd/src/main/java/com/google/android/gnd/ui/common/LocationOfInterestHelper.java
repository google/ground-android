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
import com.google.android.gnd.model.locationofinterest.GeoJsonLocationOfInterest;
import com.google.android.gnd.model.locationofinterest.LocationOfInterest;
import java8.util.Optional;
import javax.inject.Inject;
import org.jetbrains.annotations.Nullable;

/** Common logic for formatting attributes of {@link LocationOfInterest} for display to the user. */
public class LocationOfInterestHelper {

  private final Resources resources;

  @Inject
  LocationOfInterestHelper(Resources resources) {
    this.resources = resources;
  }

  public String getCreatedBy(Optional<LocationOfInterest> locationOfInterest) {
    return getUserName(locationOfInterest)
        .map(name -> resources.getString(R.string.added_by, name))
        .orElse("");
  }

  public String getLabel(Optional<LocationOfInterest> locationOfInterest) {
    return getCaption(locationOfInterest)
        .orElseGet(() -> locationOfInterest.map(this::getLocationOfInterestType).orElse(""));
  }

  private String getLocationOfInterestType(LocationOfInterest locationOfInterest) {
    if (locationOfInterest.isGeoJson() || locationOfInterest.isPolygon()) {
      return resources.getString(R.string.polygon);
    } else {
      return resources.getString(R.string.point);
    }
  }

  public String getSubtitle(Optional<LocationOfInterest> locationOfInterest) {
    return locationOfInterest
        .map(f -> resources.getString(R.string.layer_label_format, f.getJob().getName()))
        .orElse("");
  }

  private Optional<String> getUserName(Optional<LocationOfInterest> locationOfInterest) {
    return locationOfInterest
        .map(LocationOfInterest::getCreated)
        .map(AuditInfo::getUser)
        .map(User::getDisplayName);
  }

  private Optional<String> getCaption(Optional<LocationOfInterest> locationOfInterest) {
    // TODO(#793): Allow user-defined LOI names for other LOI types.
    return locationOfInterest
        .map(this::getCaption)
        .map(String::trim)
        .filter(caption -> !caption.isEmpty());
  }

  @Nullable
  private String getCaption(LocationOfInterest locationOfInterest) {
    if (locationOfInterest.isGeoJson()) {
      return getGeoJsonCaption((GeoJsonLocationOfInterest) locationOfInterest);
    }
    return locationOfInterest.getCaption();
  }

  private String getGeoJsonCaption(GeoJsonLocationOfInterest locationOfInterest) {
    String caption = locationOfInterest.getCaptionFromProperties();
    return caption.isEmpty()
        ? getLocationOfInterestType(locationOfInterest) + " " + locationOfInterest.getIdFromProperties()
        : caption;
  }
}
