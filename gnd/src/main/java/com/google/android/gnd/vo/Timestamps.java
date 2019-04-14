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

package com.google.android.gnd.vo;

import com.google.auto.value.AutoValue;
import java.util.Date;
import javax.annotation.Nullable;

@AutoValue
public abstract class Timestamps {

  public static final Timestamps DEFAULT_INSTANCE = Timestamps.newBuilder().build();

  @Nullable
  public abstract Date getCreated();

  @Nullable
  public abstract Date getModified();

  public static Builder newBuilder() {
    return new AutoValue_Timestamps.Builder();
  }

  public static Timestamps getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setCreated(@Nullable Date newCreated);

    public abstract Builder setModified(@Nullable Date newModified);

    public abstract Timestamps build();
  }
}
