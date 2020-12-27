/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.editobservation;

import android.content.Context;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.observation.Response;
import com.google.auto.value.AutoValue;
import java8.util.Optional;
import java8.util.function.Consumer;

@AutoValue
public abstract class SelectDialogFactory {

  public static Builder builder() {
    return new AutoValue_SelectDialogFactory.Builder();
  }

  public abstract Context getContext();

  public abstract String getTitle();

  public abstract MultipleChoice getMultipleChoice();

  public abstract Optional<Response> getCurrentValue();

  public abstract Consumer<Optional<Response>> getValueConsumer();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setContext(Context context);

    public abstract Builder setTitle(String title);

    public abstract Builder setMultipleChoice(MultipleChoice multipleChoice);

    public abstract Builder setCurrentValue(Optional<Response> response);

    public abstract Builder setValueConsumer(Consumer<Optional<Response>> consumer);

    public abstract SelectDialogFactory build();
  }
}
