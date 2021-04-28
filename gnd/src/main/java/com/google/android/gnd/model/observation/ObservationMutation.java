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

package com.google.android.gnd.model.observation;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.form.Form;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Represents a mutation of a observation performed on the local device. Mutations are queued
 * locally by the UI, and are dequeued and sent to the remote data store by the background data sync
 * service.
 */
@AutoValue
public abstract class ObservationMutation extends Mutation<ObservationMutation.Builder> {

  /** Returns the UUID of the observation being modified. */
  public abstract String getObservationId();

  /** Returns the form associated with this observation. */
  public abstract Form getForm();

  /** Returns list of changes to responses included in this observation mutation. */
  public abstract ImmutableList<ResponseDelta> getResponseDeltas();

  @Override
  public abstract Builder toBuilder();

  @Override
  public String toString() {
    return super.toString() + " deltas=" + getResponseDeltas();
  }

  /** Returns the mutations of type {@link ObservationMutation} contained in the specified list. */
  public static ImmutableList<ObservationMutation> filter(ImmutableList<Mutation> mutations) {
    return stream(mutations)
        .filter(ObservationMutation.class::isInstance)
        .map(ObservationMutation.class::cast)
        .collect(toImmutableList());
  }

  /**
   * Returns the ids of mutations of type {@link ObservationMutation} contained in the specified
   * list.
   */
  public static ImmutableList<Long> ids(ImmutableList<? extends Mutation> mutations) {
    return stream(mutations)
        .filter(ObservationMutation.class::isInstance)
        .map(Mutation::getId)
        .collect(toImmutableList());
  }

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static Builder builder() {
    return new AutoValue_ObservationMutation.Builder().setRetryCount(0);
  }

  @AutoValue.Builder
  public abstract static class Builder extends Mutation.Builder<Builder> {

    public abstract Builder setObservationId(String newObservationId);

    public abstract Builder setForm(Form newForm);

    public abstract Builder setResponseDeltas(ImmutableList<ResponseDelta> newResponseDeltas);

    @Override
    public abstract ObservationMutation build();
  }
}
