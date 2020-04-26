package com.google.android.gnd.ui.editobservation.field;

import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;

public class TextFieldView extends FieldView {

  public TextFieldView(ViewModelFactory viewModelFactory, Field field, AbstractFragment fragment) {
    super(viewModelFactory, field, fragment);
  }

  @Override
  public void onCreateView() {
    TextInputFieldBinding binding = TextInputFieldBinding.inflate(getLayoutInflater(), this, true);
    binding.setViewModel(getEditObservationViewModel());
    binding.setField(field);
    binding.setLifecycleOwner(getLifecycleOwner());
  }
}
