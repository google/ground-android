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

package com.google.android.gnd.ui.map;

import androidx.annotation.Dimension;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.layer.Style;
import com.google.auto.value.AutoValue;
import org.json.JSONObject;

@AutoValue
public abstract class MapGeoJson extends MapFeature {

  public static Builder newBuilder() {
    return new AutoValue_MapGeoJson.Builder();
  }

  public abstract String getId();

  public abstract JSONObject getGeoJson();

  public abstract Style getStyle();

  public abstract @Dimension int getStrokeWidth();

  // TODO: Just store the ID and pull the feature when needed.
  public abstract Feature getFeature();

  public abstract Builder toBuilder();

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof MapGeoJson) {
      MapGeoJson that = (MapGeoJson) o;
      return this.getId().equals(that.getId())
          && this.getGeoJson().toString().equals(that.getGeoJson().toString())
          && this.getStyle().equals(that.getStyle())
          && this.getStrokeWidth() == that.getStrokeWidth()
          && this.getFeature().equals(that.getFeature());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hc = 1;
    hc *= 1000003;
    hc ^= getId().hashCode();
    hc *= 1000003;
    hc ^= getGeoJson().toString().hashCode();
    hc *= 1000003;
    hc ^= getStyle().hashCode();
    hc *= 1000003;
    hc ^= getStrokeWidth();
    hc *= 1000003;
    hc ^= getFeature().hashCode();
    return hc;
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setGeoJson(JSONObject newGeoJson);

    public abstract Builder setStyle(Style style);

    public abstract Builder setStrokeWidth(@Dimension int newStrokeWidth);

    public abstract Builder setFeature(Feature feature);

    public abstract MapGeoJson build();
  }
}
