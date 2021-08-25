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

package com.google.android.gnd.model.feature;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.Mutation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;

/**
 * Represents a mutation of a feature performed on the local device. Mutations are queued locally by
 * the UI, and are dequeued and sent to the remote data store by the background data sync service.
 */
@AutoValue
public abstract class FeatureMutation extends Mutation<FeatureMutation.Builder> {

  /**
   * Indicates the new location of the feature. If empty, indicates no change to the feature's
   * location.
   */
  public abstract Optional<Point> getNewLocation();

  public abstract Optional<ImmutableList<Point>> getNewPolygonVertices();

  @Override
  public abstract Builder toBuilder();

  /** Returns the mutations of type {@link FeatureMutation} contained in the specified list. */
  public static ImmutableList<FeatureMutation> filter(ImmutableList<Mutation> mutations) {
    return stream(mutations)
        .filter(FeatureMutation.class::isInstance)
        .map(FeatureMutation.class::cast)
        .collect(toImmutableList());
  }

  /**
   * Returns the ids of mutations of type {@link FeatureMutation} contained in the specified list.
   */
  public static ImmutableList<Long> ids(ImmutableList<? extends Mutation> mutations) {
    return stream(mutations)
        .filter(FeatureMutation.class::isInstance)
        .map(Mutation::getId)
        .collect(toImmutableList());
  }

  public static Builder builder() {
    return new AutoValue_FeatureMutation.Builder()
        .setRetryCount(0)
        .setSyncStatus(SyncStatus.UNKNOWN)
        .setNewLocation(Optional.empty())
        .setNewPolygonVertices(Optional.empty());
  }

  @AutoValue.Builder
  public abstract static class Builder extends Mutation.Builder<Builder> {

    public abstract Builder setNewLocation(Optional<Point> newNewLocation);

    public abstract Builder setNewPolygonVertices(Optional<ImmutableList<Point>> polygonVertices);

    @Override
    public abstract FeatureMutation build();
  }
}
