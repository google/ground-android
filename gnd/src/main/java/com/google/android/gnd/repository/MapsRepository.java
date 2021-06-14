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

package com.google.android.gnd.repository;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gnd.persistence.local.LocalValueStore;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MapsRepository {

  private static final int DEFAULT_MAP_TYPE = GoogleMap.MAP_TYPE_NORMAL;

  private final LocalValueStore localValueStore;

  @Inject
  public MapsRepository(LocalValueStore localValueStore) {
    this.localValueStore = localValueStore;
  }

  public void saveMapType(int type) {
    localValueStore.saveMapType(type);
  }

  public int getSavedMapType() {
    return localValueStore.getSavedMapType(DEFAULT_MAP_TYPE);
  }
}
