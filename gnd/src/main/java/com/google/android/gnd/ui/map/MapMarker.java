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

package com.google.android.gnd.ui.map;

import com.google.android.gnd.ui.MapIcon;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Point;
import com.google.auto.value.AutoValue;
import java8.util.Optional;
import javax.annotation.Nullable;

@AutoValue
public abstract class MapMarker {

  public abstract String getId();

  public abstract Point getPosition();

  public abstract MapIcon getIcon();

  @Nullable
  public abstract Object getObject();

  public Optional<Feature> getFeature() {
    return getObject() != null && getObject() instanceof Feature
        ? Optional.of((Feature) getObject())
        : Optional.empty();
  }

  public static Builder newBuilder() {
    return new AutoValue_MapMarker.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setPosition(Point newPosition);

    public abstract Builder setIcon(MapIcon newIcon);

    public abstract Builder setObject(Object newObject);

    public abstract MapMarker build();
  }
}
