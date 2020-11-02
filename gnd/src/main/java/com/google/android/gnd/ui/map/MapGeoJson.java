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

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setGeoJson(JSONObject newGeoJson);

    public abstract Builder setStyle(Style style);

    public abstract MapGeoJson build();
  }
}
