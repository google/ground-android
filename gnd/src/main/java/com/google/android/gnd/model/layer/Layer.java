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

package com.google.android.gnd.model.layer;

import com.google.android.gnd.model.form.Form;
import com.google.auto.value.AutoValue;
import java8.util.Optional;

@AutoValue
public abstract class Layer {
  public abstract String getId();

  public abstract String getName();

  public abstract Style getDefaultStyle();

  public abstract Optional<Form> getForm();

  public Optional<Form> getForm(String formId) {
    return getForm().filter(form -> form.getId().equals(formId));
  }

  public static Builder newBuilder() {
    return new AutoValue_Layer.Builder().setForm(Optional.empty());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setName(String newName);

    public abstract Builder setDefaultStyle(Style newDefaultStyle);

    public abstract Builder setForm(Optional<Form> form);

    public Builder setForm(Form form) {
      return setForm(Optional.of(form));
    }

    public abstract Layer build();
  }
}
