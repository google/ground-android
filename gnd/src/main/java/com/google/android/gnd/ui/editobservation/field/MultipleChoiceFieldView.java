package com.google.android.gnd.ui.editobservation.field;

import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;

public class MultipleChoiceFieldView extends FieldView {

  public MultipleChoiceFieldView(
      ViewModelFactory viewModelFactory,
      EditObservationFragment editObservationFragment,
      Field field) {
    super(viewModelFactory, field, editObservationFragment);
    MultipleChoiceInputFieldBinding binding =
        MultipleChoiceInputFieldBinding.inflate(getLayoutInflater(), this, true);
    binding.setFragment(editObservationFragment);
    binding.setViewModel(getEditObservationViewModel());
    binding.setLifecycleOwner(getLifecycleOwner());
    binding.setField(field);
  }
}
