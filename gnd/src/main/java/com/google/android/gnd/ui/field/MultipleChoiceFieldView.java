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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.editobservation.EditObservationFragmentArgs;
import java8.util.Optional;
import java8.util.function.Consumer;

public class MultipleChoiceFieldView extends FieldView {

  private AlertDialog dialog;

  public MultipleChoiceFieldView(
      AbstractFragment fragment,
      Field field,
      Optional<Response> response,
      EditObservationFragmentArgs args) {
    super(fragment, field, response, args);
  }

  @Override
  public void onCreateView() {
    if (isEditMode()) {
      MultipleChoiceInputFieldBinding binding =
          MultipleChoiceInputFieldBinding.inflate(getLayoutInflater(), this, true);
      binding.setFieldView(this);
      binding.setLifecycleOwner(getLifecycleOwner());
    }
  }

  public void onShowDialog() {
    dialog = getDialog(this::setResponse);
    if (dialog != null) {
      dialog.show();
    }
  }

  @Nullable
  private AlertDialog getDialog(Consumer<Optional<Response>> consumer) {
    switch (field.getMultipleChoice().getCardinality()) {
      case SELECT_ONE:
        return new SingleSelectDialogFactory(getContext()).create(field, currentResponse, consumer);
      case SELECT_MULTIPLE:
        return new MultiSelectDialogFactory(getContext()).create(field, currentResponse, consumer);
      default:
        throw new IllegalStateException(
            "Unknown cardinality: " + field.getMultipleChoice().getCardinality());
    }
  }

  @Override
  public void onPause() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
    super.onPause();
  }

  @Override
  public Optional<Response> getResponse() {
    return currentResponse != null ? currentResponse : Optional.empty();
  }
}
