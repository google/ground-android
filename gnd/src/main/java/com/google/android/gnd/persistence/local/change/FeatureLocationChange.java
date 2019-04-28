/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.persistence.local.change;

import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Point;
import com.google.auto.value.AutoValue;

/** Represents a change in a feature's location. */
@AutoValue
public abstract class FeatureLocationChange implements LocalChange<Feature> {
  /** Returns the feature's location before this change is applied. */
  public abstract Point getOldLocation();

  /** Returns the feature's location after this change is applied. */
  public abstract Point getNewLocation();

  @Override
  public Class<Feature> getEntityType() {
    return Feature.class;
  }

  public static Builder builder() {
    return new AutoValue_FeatureLocationChange.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setChangeId(long newChangeId);

    public abstract Builder setEntityId(String newEntityId);

    public abstract Builder setUserId(String newUserId);

    public abstract Builder setOldLocation(Point newOldLocation);

    public abstract Builder setNewLocation(Point newNewLocation);

    public abstract FeatureLocationChange build();
  }
}
