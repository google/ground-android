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

import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.editobservation.EditObservationFragmentArgs;
import java8.util.Optional;

public class TextFieldView extends FieldView {

  public TextFieldView(
      AbstractFragment fragment,
      Field field,
      Optional<Response> response,
      EditObservationFragmentArgs args) {
    super(fragment, field, response, args);
  }

  @Override
  public void onCreateView() {
    if (isEditMode()) {
      TextInputFieldBinding binding =
          TextInputFieldBinding.inflate(getLayoutInflater(), this, true);
      //      binding.setFieldView(this);
      binding.setLifecycleOwner(getLifecycleOwner());
    }
  }

  @Override
  public Optional<Response> getResponse() {
    return TextResponse.fromString(getModel().getResponse());
  }
}
