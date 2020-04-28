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
import com.google.android.gnd.databinding.AddPhotoBottomSheetDialogBinding;
import com.google.android.gnd.databinding.PhotoInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PhotoFieldView extends FieldView {

  @Nullable private BottomSheetDialog bottomSheetDialog;

  public PhotoFieldView(ViewModelFactory viewModelFactory, AbstractFragment fragment, Field field) {
    super(viewModelFactory, fragment, field);
  }

  @Override
  public void onCreateView() {
    if (isEditMode()) {
      PhotoInputFieldBinding binding =
          PhotoInputFieldBinding.inflate(getLayoutInflater(), this, true);
      binding.setFieldView(this);
      binding.setLifecycleOwner(getLifecycleOwner());

      PhotoFieldViewModel photoFieldViewModel = createViewModel(PhotoFieldViewModel.class);
      photoFieldViewModel.init(field, getViewModel().getResponses());
      binding.setViewModel(photoFieldViewModel);
    }
  }

  public void onShowDialog() {
    AddPhotoBottomSheetDialogBinding binding =
        AddPhotoBottomSheetDialogBinding.inflate(getLayoutInflater());
    binding.setViewModel(getViewModel());
    binding.setField(field);

    if (bottomSheetDialog == null) {
      bottomSheetDialog = new BottomSheetDialog(getContext());
      bottomSheetDialog.setContentView(binding.getRoot());
    }

    if (!bottomSheetDialog.isShowing()) {
      bottomSheetDialog.show();
    }
  }

  @Override
  public void onPause() {
    if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
      bottomSheetDialog.dismiss();
    }

    super.onPause();
  }
}
