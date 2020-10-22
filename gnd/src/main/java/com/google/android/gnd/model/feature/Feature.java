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
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.layer.Layer;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import javax.annotation.Nullable;

@AutoValue
public abstract class Feature {
  // TODO: Use builder() or newBuilder() consistently.
  public static Builder newBuilder() {
    return new AutoValue_Feature.Builder();
  }

  public boolean isPoint() {
    return getPoint() != null;
  }

  public boolean isPolygon() {
    return getPolygon() != null;
  }

  @NonNull
  public abstract String getId();

  public abstract Project getProject();

  public abstract Layer getLayer();

  @Nullable
  public abstract String getCustomId();

  @Nullable
  public abstract String getCaption();

  @Nullable
  public abstract Point getPoint();

  @Nullable
  public abstract Polygon getPolygon();

  /** Returns the user and time audit info pertaining to the creation of this feature. */
  public abstract AuditInfo getCreated();

  /** Returns the user and time audit info pertaining to the last modification of this feature. */
  public abstract AuditInfo getLastModified();

  public abstract Builder toBuilder();

  @Memoized
  @Override
  public abstract int hashCode();

  @AutoValue.Builder
  public abstract static class Builder {
    // TODO: Use newFoo or foo consistently.
    public abstract Builder setId(@NonNull String newId);

    public abstract Builder setProject(@NonNull Project project);

    public abstract Builder setLayer(@NonNull Layer newLayer);

    public abstract Builder setCustomId(@Nullable String newCustomId);

    public abstract Builder setCaption(@Nullable String newCaption);

    public abstract Builder setPoint(@Nullable Point newPoint);

    public abstract Builder setPolygon(@Nullable Polygon newPolygon);

    public abstract Builder setCreated(@NonNull AuditInfo newCreated);

    public abstract Builder setLastModified(@NonNull AuditInfo newLastModified);

    public abstract Feature build();
  }
}
