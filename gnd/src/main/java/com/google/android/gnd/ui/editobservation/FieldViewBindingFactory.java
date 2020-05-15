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

import static com.google.android.gnd.ui.util.ViewUtil.assignGeneratedId;

import android.view.LayoutInflater;
import android.widget.LinearLayout;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import com.google.android.gnd.BR;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;

/** Generates {@link ViewDataBinding} instance for a given {@link Field}. */
public final class FieldViewBindingFactory {

  @NonNull private final EditObservationFragment fragment;
  @NonNull private final ViewModelFactory viewModelFactory;

  FieldViewBindingFactory(
      @NonNull EditObservationFragment fragment, @NonNull ViewModelFactory viewModelFactory) {
    this.fragment = fragment;
    this.viewModelFactory = viewModelFactory;
  }

  private LayoutInflater getLayoutInflater() {
    return fragment.getLayoutInflater();
  }

  private <VM extends AbstractFieldViewModel> VM create(
      Class<VM> modelClass, @LayoutRes int layoutId, LinearLayout formLayout) {
    VM viewModel = viewModelFactory.create(modelClass);

    ViewDataBinding binding =
        DataBindingUtil.inflate(getLayoutInflater(), layoutId, formLayout, true);
    binding.setLifecycleOwner(fragment);
    binding.setVariable(BR.viewModel, viewModel);
    assignGeneratedId(binding.getRoot());

    return viewModel;
  }

  /** Returns {@link AbstractFieldViewModel} attached to the generated {@link ViewDataBinding}. */
  public AbstractFieldViewModel create(Field.Type fieldType, LinearLayout formLayout) {
    switch (fieldType) {
      case TEXT:
        return create(TextFieldViewModel.class, R.layout.text_input_field, formLayout);
      case MULTIPLE_CHOICE:
        return create(
            MultipleChoiceFieldViewModel.class, R.layout.multiple_choice_input_field, formLayout);
      case PHOTO:
        return create(PhotoFieldViewModel.class, R.layout.photo_input_field, formLayout);
      default:
        throw new IllegalArgumentException("Unsupported field type: " + fieldType);
    }
  }
}
