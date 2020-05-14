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

import android.view.LayoutInflater;
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

  public ViewDataBinding create(Field field) {
    switch (field.getType()) {
      case TEXT:
        return createTextFieldBinding(field);
      case MULTIPLE_CHOICE:
        return createMultipleChoiceFieldBinding(field);
      case PHOTO:
        return createPhotoFieldBinding(field);
      default:
        throw new IllegalArgumentException("Unsupported field type: " + field.getType());
    }
  }

  private ViewDataBinding createPhotoFieldBinding(Field field) {
    PhotoInputFieldBinding binding =
        PhotoInputFieldBinding.inflate(getLayoutInflater(), null, false);
    binding.setLifecycleOwner(fragment);
    binding.setField(field);
    binding.setFragment(fragment);

    PhotoFieldViewModel photoFieldViewModel = viewModelFactory.create(PhotoFieldViewModel.class);
    photoFieldViewModel.init(field, viewModel.getResponses());
    binding.setViewModel(photoFieldViewModel);
    return binding;
  }

  private ViewDataBinding createMultipleChoiceFieldBinding(Field field) {
    MultipleChoiceInputFieldBinding binding =
        MultipleChoiceInputFieldBinding.inflate(getLayoutInflater(), null, false);
    binding.setFragment(fragment);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(fragment);
    binding.setField(field);
    return binding;
  }

  private ViewDataBinding createTextFieldBinding(Field field) {
    TextInputFieldBinding binding = TextInputFieldBinding.inflate(getLayoutInflater(), null, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(fragment);
    binding.setField(field);
    return binding;
  }
}
