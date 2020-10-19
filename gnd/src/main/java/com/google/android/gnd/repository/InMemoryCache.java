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

package com.google.android.gnd.repository;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.feature.Feature;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java8.util.Optional;
import javax.inject.Inject;

/** Ephemeral storage of application state. This can be destroyed without notice. */
public class InMemoryCache {
  // TODO: Store map vector objects here instead of Feature objects.
  @NonNull
  private final Map<String, Feature> features;

  @Inject
  public InMemoryCache() {
    this.features = new LinkedHashMap<>();
  }

  public synchronized void putFeature(@NonNull Feature feature) {
    features.put(feature.getId(), feature);
  }

  public void removeFeature(String id) {
    features.remove(id);
  }

  public synchronized ImmutableSet<Feature> getFeatures() {
    return ImmutableSet.copyOf(features.values());
  }

  /** Clears the cache. */
  public void clear() {
    features.clear();
  }

  public Optional<Feature> getFeature(String featureId) {
    return Optional.ofNullable(features.get(featureId));
  }
}
