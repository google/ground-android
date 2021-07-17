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

package com.google.android.gnd.model.feature;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation.SyncStatus;
import com.google.android.gnd.model.Mutation.Type;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.layer.Layer;
import java.util.Date;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

/** Base class for user-defined features shown on map. */
public abstract class Feature<B extends Feature.Builder> {
  public boolean isPoint() {
    return this instanceof PointFeature;
  }

  public boolean isGeoJson() {
    return this instanceof GeoJsonFeature;
  }

  public boolean isPolygon() {
    return this instanceof PolygonFeature;
  }

  @NonNull
  public abstract String getId();

  public abstract Project getProject();

  public abstract Layer getLayer();

  @Nullable
  public abstract String getCustomId();

  @Nullable
  public abstract String getCaption();

  /** Returns the user and time audit info pertaining to the creation of this feature. */
  public abstract AuditInfo getCreated();

  /** Returns the user and time audit info pertaining to the last modification of this feature. */
  public abstract AuditInfo getLastModified();

  @OverridingMethodsMustInvokeSuper
  public FeatureMutation toMutation(Type type, String userId) {
    return FeatureMutation.builder()
        .setType(type)
        .setSyncStatus(SyncStatus.PENDING)
        .setProjectId(getProject().getId())
        .setFeatureId(getId())
        .setLayerId(getLayer().getId())
        .setUserId(userId)
        .setClientTimestamp(new Date())
        .build();
  }

  public abstract static class Builder<T extends Feature.Builder> {
    // TODO: Use newFoo or foo consistently.
    public abstract T setId(@NonNull String newId);

    public abstract T setProject(@NonNull Project project);

    public abstract T setLayer(@NonNull Layer newLayer);

    public abstract T setCustomId(@Nullable String newCustomId);

    public abstract T setCaption(@Nullable String newCaption);

    public abstract T setCreated(@NonNull AuditInfo newCreated);

    public abstract T setLastModified(@NonNull AuditInfo newLastModified);
  }
}
