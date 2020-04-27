package com.google.android.gnd.ui.editobservation.field;

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
      photoFieldViewModel.init(field, getEditObservationViewModel().getResponses());
      binding.setViewModel(photoFieldViewModel);
    }
  }

  public void onShowDialog() {
    AddPhotoBottomSheetDialogBinding binding =
        AddPhotoBottomSheetDialogBinding.inflate(getLayoutInflater());
    binding.setViewModel(getEditObservationViewModel());
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
