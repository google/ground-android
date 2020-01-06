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
  // TODO: Replace Optionals with Nullables in VOs for consistency .
  @NonNull
  public abstract String getId();

  public abstract Project getProject();

  public abstract Layer getLayer();

  @Nullable
  public abstract String getCustomId();

  @Nullable
  public abstract String getCaption();

  public abstract Point getPoint();

  /** Returns the user and time audit info pertaining to the creation of this observation. */
  public abstract AuditInfo getCreated();

  /**
   * Returns the user and time audit info pertaining to the last modification of this observation.
   */
  public abstract AuditInfo getLastModified();

  public String getTitle() {
    return getCaption() == null || getCaption().isEmpty()
        ? getLayer().getItemLabel()
        : getCaption();
  }

  public String getSubtitle() {
    // TODO: Implement subtitle logic.
    return "";
  }

  public abstract Builder toBuilder();

  @Memoized
  @Override
  public abstract int hashCode();

  // TODO: Use builder() or newBuilder() consistently.
  public static Builder newBuilder() {
    return new AutoValue_Feature.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    // TODO: Use newFoo or foo consistently.
    public abstract Builder setId(String newId);

    public abstract Builder setProject(Project project);

    public abstract Builder setLayer(Layer newLayer);

    public abstract Builder setCustomId(String newCustomId);

    public abstract Builder setCaption(String newCaption);

    public abstract Builder setPoint(Point newPoint);

    public abstract Builder setCreated(AuditInfo newCreated);

    public abstract Builder setLastModified(AuditInfo newLastModified);

    public abstract Feature build();
  }
}
