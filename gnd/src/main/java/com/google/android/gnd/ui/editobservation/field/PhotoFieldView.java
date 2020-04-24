package com.google.android.gnd.ui.editobservation.field;

import com.google.android.gnd.databinding.PhotoInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import com.google.android.gnd.ui.editobservation.PhotoFieldViewModel;

public class PhotoFieldView extends FieldView {

  public PhotoFieldView(
      ViewModelFactory viewModelFactory,
      EditObservationFragment editObservationFragment,
      Field field) {
    super(viewModelFactory, field, editObservationFragment);
    PhotoInputFieldBinding binding =
        PhotoInputFieldBinding.inflate(getLayoutInflater(), this, false);
    binding.setFragment(editObservationFragment);
    binding.setField(field);
    binding.setLifecycleOwner(getLifecycleOwner());

    PhotoFieldViewModel photoFieldViewModel = createViewModel(PhotoFieldViewModel.class);
    photoFieldViewModel.init(field, getEditObservationViewModel().getResponses());
    binding.setViewModel(photoFieldViewModel);
  }
}
