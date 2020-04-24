package com.google.android.gnd.ui.editobservation.field;

import androidx.appcompat.app.AlertDialog;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.MultipleChoice.Cardinality;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import com.google.android.gnd.ui.editobservation.MultiSelectDialogFactory;
import com.google.android.gnd.ui.editobservation.SingleSelectDialogFactory;
import java8.util.Optional;

public class MultipleChoiceFieldView extends FieldView {

  private AlertDialog dialog;

  public MultipleChoiceFieldView(
      ViewModelFactory viewModelFactory,
      EditObservationFragment editObservationFragment,
      Field field) {
    super(viewModelFactory, field, editObservationFragment);
    MultipleChoiceInputFieldBinding binding =
        MultipleChoiceInputFieldBinding.inflate(getLayoutInflater(), this, true);
    binding.setFieldView(this);
    binding.setViewModel(getEditObservationViewModel());
    binding.setLifecycleOwner(getLifecycleOwner());
    binding.setField(field);
  }

  public void onShowDialog() {
    Cardinality cardinality = field.getMultipleChoice().getCardinality();
    Optional<Response> currentResponse = getEditObservationViewModel().getResponse(field.getId());
    switch (cardinality) {
      case SELECT_MULTIPLE:
        dialog =
            new MultiSelectDialogFactory(getContext())
                .create(
                    field,
                    currentResponse,
                    r -> getEditObservationViewModel().onResponseChanged(field, r));
        break;
      case SELECT_ONE:
        dialog =
            new SingleSelectDialogFactory(getContext())
                .create(
                    field,
                    currentResponse,
                    r -> getEditObservationViewModel().onResponseChanged(field, r));
        break;
      default:
        throw new IllegalStateException("Unknown cardinality: " + cardinality);
    }
    if (dialog != null) {
      dialog.show();
    }
  }

  @Override
  public void onPause() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
    super.onPause();
  }
}
