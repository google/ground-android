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

package com.google.android.gnd.persistence.shared;

import com.google.android.gnd.vo.Point;
import com.google.auto.value.AutoValue;
import java8.util.Optional;

/**
 * Represents a mutation of a feature performed on the local device. Mutations are queued locally by
 * the UI, and are dequeued and sent to the remote data store by the background data sync service.
 */
@AutoValue
public abstract class FeatureMutation extends Mutation {

  /** Returns the UUID the feature being modified. */
  public abstract String getFeatureId();

  /** Returns the UUID of the feature type being modified. */
  public abstract String getFeatureTypeId();

  /**
   * Indicates the new location of the feature. If empty, indicates no change to the feature's
   * location.
   */
  public abstract Optional<Point> getNewLocation();

  public static Builder builder() {
    return new AutoValue_FeatureMutation.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder extends Mutation.Builder<Builder> {

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setFeatureTypeId(String newFeatureTypeId);

    public abstract Builder setNewLocation(Optional<Point> newNewLocation);

    public abstract FeatureMutation build();
  }
}
