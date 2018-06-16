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

import com.google.android.gnd.vo.Place;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;

/** Ephemeral storage of application state. This can be destroyed without notice. */
public class InMemoryCache {
  private Map<String, Place> places;

  @Inject
  public InMemoryCache() {
    this.places = new LinkedHashMap<>();
  }

  public synchronized void putPlace(Place place) {
    places.put(place.getId(), place);
  }

  public void removePlace(String id) {
    places.remove(id);
  }

  public synchronized ImmutableSet<Place> getPlaces() {
    return ImmutableSet.copyOf(places.values());
  }

  public synchronized void clear() {
    places.clear();
  }
}
