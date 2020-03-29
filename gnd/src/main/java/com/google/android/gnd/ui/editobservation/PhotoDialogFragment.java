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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gnd.databinding.EditObservationBottomSheetBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Generates a bottom sheet with options for adding a photo to the observation.
 *
 * <p>Since one observation can have multiple photo fields, hence it is must to map the fieldId
 * along with each request.}
 */
public class PhotoDialogFragment extends BottomSheetDialogFragment {

  public static final String TAG = PhotoDialogFragment.class.getSimpleName();
  private EditObservationBottomSheetBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = EditObservationBottomSheetBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  void init(EditObservationViewModel viewModel, Field field) {
    binding.setField(field);
    binding.setViewModel(viewModel);
  }
}
