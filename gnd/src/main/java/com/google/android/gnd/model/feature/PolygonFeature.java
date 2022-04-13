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

import com.google.android.gnd.model.mutation.FeatureMutation;
import com.google.android.gnd.model.mutation.Mutation.Type;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;

/** User-defined map feature consisting of a polygon. */
@AutoValue
public abstract class PolygonFeature extends Feature<PolygonFeature.Builder> {

  public static Builder builder() {
    return new AutoValue_PolygonFeature.Builder();
  }

  public abstract ImmutableList<Point> getVertices();

  @Override
  public FeatureMutation toMutation(Type type, String userId) {
    return super.toMutation(type, userId).toBuilder().setPolygonVertices(getVertices()).build();
  }

  @Memoized
  @Override
  public abstract int hashCode();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder extends Feature.Builder<Builder> {

    public abstract Builder setVertices(ImmutableList<Point> newVertices);

    public abstract PolygonFeature build();
  }
}
