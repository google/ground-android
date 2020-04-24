package com.google.android.gnd.ui.editobservation.field;

import android.content.Context;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import com.google.android.gnd.ui.editobservation.EditObservationViewModel;

public class MultipleChoiceFieldView extends FieldView {

  public MultipleChoiceFieldView(
      Context context,
      EditObservationFragment editObservationFragment,
      EditObservationViewModel viewModel,
      Field field,
      LifecycleOwner lifecycleOwner) {
    super(context);
    MultipleChoiceInputFieldBinding binding =
        MultipleChoiceInputFieldBinding.inflate(getLayoutInflater(), this, true);
    binding.setFragment(editObservationFragment);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(lifecycleOwner);
    binding.setField(field);
  }
}
