package com.google.android.gnd.ui.editobservation.field;

import android.content.Context;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.editobservation.EditObservationViewModel;

public class TextFieldView extends FieldView {

  public TextFieldView(
      Context context,
      EditObservationViewModel viewModel,
      Field field,
      LifecycleOwner lifecycleOwner) {
    super(context);
    TextInputFieldBinding binding = TextInputFieldBinding.inflate(getLayoutInflater(), this, true);
    binding.setViewModel(viewModel);
    binding.setField(field);
    binding.setLifecycleOwner(lifecycleOwner);
  }
}
