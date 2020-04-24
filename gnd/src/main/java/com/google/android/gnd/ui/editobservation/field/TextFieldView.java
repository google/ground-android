package com.google.android.gnd.ui.editobservation.field;

import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;

public class TextFieldView extends FieldView {

  public TextFieldView(
      ViewModelFactory viewModelFactory,
      EditObservationFragment editObservationFragment,
      Field field) {
    super(viewModelFactory, field, editObservationFragment);
    TextInputFieldBinding binding = TextInputFieldBinding.inflate(getLayoutInflater(), this, true);
    binding.setViewModel(getEditObservationViewModel());
    binding.setField(field);
    binding.setLifecycleOwner(getLifecycleOwner());
  }
}
