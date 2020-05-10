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

package com.google.android.gnd.ui.field;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragmentArgs;
import java8.util.Optional;

public class FieldFactory {

  private final AbstractFragment fragment;
  private final ViewModelFactory viewModelFactory;
  private final EditObservationFragmentArgs args;

  public FieldFactory(
      AbstractFragment fragment,
      ViewModelFactory viewModelFactory,
      EditObservationFragmentArgs args) {
    this.fragment = fragment;
    this.viewModelFactory = viewModelFactory;
    this.args = args;
  }

  @NonNull
  public FieldView createFieldView(Field field, Optional<Response> response) {
    switch (field.getType()) {
      case TEXT:
        return new TextFieldView(fragment, field, response, args);
      case MULTIPLE_CHOICE:
        return new MultipleChoiceFieldView(fragment, field, response, args);
      case PHOTO:
        return new PhotoFieldView(viewModelFactory, fragment, field, response, args);
      default:
        throw new IllegalStateException("Unimplemented field type: " + field.getType());
    }
  }
}
