package com.google.android.gnd.ui.editobservation.field;

import android.content.Context;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.gnd.databinding.PhotoInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import com.google.android.gnd.ui.editobservation.EditObservationViewModel;
import com.google.android.gnd.ui.editobservation.PhotoFieldViewModel;

public class PhotoFieldView extends FieldView {

  public PhotoFieldView(
      Context context,
      ViewModelFactory viewModelFactory,
      EditObservationFragment editObservationFragment,
      EditObservationViewModel viewModel,
      Field field,
      LifecycleOwner lifecycleOwner) {
    super(context);
    PhotoInputFieldBinding binding =
        PhotoInputFieldBinding.inflate(getLayoutInflater(), this, false);
    binding.setFragment(editObservationFragment);
    binding.setField(field);
    binding.setLifecycleOwner(lifecycleOwner);

    PhotoFieldViewModel photoFieldViewModel = viewModelFactory.create(PhotoFieldViewModel.class);
    photoFieldViewModel.init(field, viewModel.getResponses());
    binding.setViewModel(photoFieldViewModel);
  }
}
