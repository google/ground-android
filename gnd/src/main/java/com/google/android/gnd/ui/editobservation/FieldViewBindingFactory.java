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
import androidx.annotation.NonNull;
import androidx.databinding.ViewDataBinding;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.databinding.PhotoInputFieldBinding;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;

/** Generates {@link ViewDataBinding} instance for a given {@link Field}. */
public final class FieldViewBindingFactory {

  @NonNull private final EditObservationFragment fragment;
  @NonNull private final EditObservationViewModel viewModel;
  @NonNull private final ViewModelFactory viewModelFactory;

  FieldViewBindingFactory(
      @NonNull EditObservationFragment fragment,
      @NonNull EditObservationViewModel viewModel,
      @NonNull ViewModelFactory viewModelFactory) {
    this.fragment = fragment;
    this.viewModel = viewModel;
    this.viewModelFactory = viewModelFactory;
  }

  private LayoutInflater getLayoutInflater() {
    return fragment.getLayoutInflater();
  }

  public ViewDataBinding create(Field field, LinearLayout formLayout) {
    ViewDataBinding viewDataBinding = null;
    switch (field.getType()) {
      case TEXT:
        viewDataBinding = createTextFieldBinding(field, formLayout);
        break;
      case MULTIPLE_CHOICE:
        viewDataBinding = createMultipleChoiceFieldBinding(field, formLayout);
        break;
      case PHOTO:
        viewDataBinding = createPhotoFieldBinding(field, formLayout);
        break;
      default:
        throw new IllegalArgumentException("Unsupported field type: " + field.getType());
    }

    assignGeneratedId(viewDataBinding.getRoot());
    return viewDataBinding;
  }

  private ViewDataBinding createPhotoFieldBinding(Field field, LinearLayout formLayout) {
    PhotoInputFieldBinding binding =
        PhotoInputFieldBinding.inflate(getLayoutInflater(), formLayout, true);
    binding.setLifecycleOwner(fragment);
    binding.setField(field);
    binding.setFragment(fragment);

    PhotoFieldViewModel photoFieldViewModel = viewModelFactory.create(PhotoFieldViewModel.class);
    photoFieldViewModel.init(field, viewModel.getResponses());
    binding.setViewModel(photoFieldViewModel);
    return binding;
  }

  private ViewDataBinding createMultipleChoiceFieldBinding(Field field, LinearLayout formLayout) {
    MultipleChoiceInputFieldBinding binding =
        MultipleChoiceInputFieldBinding.inflate(getLayoutInflater(), formLayout, true);
    binding.setFragment(fragment);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(fragment);
    binding.setField(field);
    return binding;
  }

  private ViewDataBinding createTextFieldBinding(Field field, LinearLayout formLayout) {
    TextInputFieldBinding binding =
        TextInputFieldBinding.inflate(getLayoutInflater(), formLayout, true);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(fragment);
    binding.setField(field);
    return binding;
  }
}
