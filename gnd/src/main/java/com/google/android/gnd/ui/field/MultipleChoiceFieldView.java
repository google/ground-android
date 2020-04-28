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

import androidx.appcompat.app.AlertDialog;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.MultipleChoice.Cardinality;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;
import java8.util.Optional;

public class MultipleChoiceFieldView extends FieldView {

  private AlertDialog dialog;

  public MultipleChoiceFieldView(
      ViewModelFactory viewModelFactory, AbstractFragment fragment, Field field) {
    super(viewModelFactory, fragment, field);
  }

  @Override
  public void onCreateView() {
    if (isEditMode()) {
      MultipleChoiceInputFieldBinding binding =
          MultipleChoiceInputFieldBinding.inflate(getLayoutInflater(), this, true);
      binding.setFieldView(this);
      binding.setViewModel(getViewModel());
      binding.setLifecycleOwner(getLifecycleOwner());
      binding.setField(field);
    }
  }

  public void onShowDialog() {
    Cardinality cardinality = field.getMultipleChoice().getCardinality();
    Optional<Response> currentResponse = getViewModel().getResponse(field.getId());
    switch (cardinality) {
      case SELECT_MULTIPLE:
        dialog =
            new MultiSelectDialogFactory(getContext())
                .create(field, currentResponse, r -> getViewModel().onResponseChanged(field, r));
        break;
      case SELECT_ONE:
        dialog =
            new SingleSelectDialogFactory(getContext())
                .create(field, currentResponse, r -> getViewModel().onResponseChanged(field, r));
        break;
      default:
        throw new IllegalStateException("Unknown cardinality: " + cardinality);
    }
    if (dialog != null) {
      dialog.show();
    }
  }

  @Override
  public void onPause() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
    super.onPause();
  }
}
