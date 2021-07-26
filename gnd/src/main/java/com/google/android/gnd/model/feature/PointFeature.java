/*
 * Copyright 2021 Google LLC
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

import com.google.android.gnd.model.Mutation.Type;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import java8.util.Optional;

/** User-defined map feature consisting of a single point. */
@AutoValue
public abstract class PointFeature extends Feature<PointFeature.Builder> {
  // TODO: Use builder() or newBuilder() consistently.
  public static Builder newBuilder() {
    return new AutoValue_PointFeature.Builder();
  }

  public abstract Point getPoint();

  @Override
  public FeatureMutation toMutation(Type type, String userId) {
    return super.toMutation(type, userId).toBuilder()
        .setNewLocation(Optional.of(getPoint()))
        .build();
  }

  @Memoized
  @Override
  public abstract int hashCode();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder extends Feature.Builder<PointFeature.Builder> {

    public abstract Builder setPoint(Point newPoint);

    public abstract PointFeature build();
  }
}
