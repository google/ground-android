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

package com.google.android.gnd.model;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.basemap.OfflineBaseMapSource;
import com.google.android.gnd.model.layer.Layer;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java8.util.Optional;

/** Configuration, schema, and ACLs for a single project. */
@AutoValue
public abstract class Project {

  public abstract String getId();

  public abstract String getTitle();

  public abstract String getDescription();

  @NonNull
  protected abstract ImmutableMap<String, Layer> getLayerMap();

  @NonNull
  public abstract ImmutableList<OfflineBaseMapSource> getOfflineBaseMapSources();

  public ImmutableList<Layer> getLayers() {
    return getLayerMap().values().asList();
  }

  public Optional<Layer> getLayer(String layerId) {
    return Optional.ofNullable(getLayerMap().get(layerId));
  }

  public static Builder newBuilder() {
    return new AutoValue_Project.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setTitle(String newTitle);

    public abstract Builder setDescription(String newDescription);

    public abstract ImmutableMap.Builder<String, Layer> layerMapBuilder();

    public Builder putLayer(String id, Layer layer) {
      layerMapBuilder().put(id, layer);
      return this;
    }

    public abstract ImmutableList.Builder<OfflineBaseMapSource> offlineBaseMapSourcesBuilder();

    public Builder addOfflineBaseMapSource(OfflineBaseMapSource source) {
      offlineBaseMapSourcesBuilder().add(source);
      return this;
    }

    public abstract Project build();
  }
}
