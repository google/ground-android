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

package com.google.android.gnd.model.basemap;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.auto.value.AutoValue;

/** An area is a contiguous set of tiles that form a geodesic rectangle. */
@AutoValue
public abstract class OfflineBaseMap {

  public abstract String getId();

  public abstract State getState();

  public abstract LatLngBounds getBounds();

  public abstract String getName();

  public abstract Builder toBuilder();

  public enum State {
    PENDING,
    IN_PROGRESS,
    DOWNLOADED,
    FAILED
  }

  public static Builder newBuilder() {
    return new AutoValue_OfflineBaseMap.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setBounds(LatLngBounds bounds);

    public abstract Builder setState(State state);

    public abstract Builder setId(String id);

    public abstract Builder setName(String name);

    public abstract OfflineBaseMap build();
  }
}
