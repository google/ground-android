package com.google.android.gnd.ui.editobservation.field;

import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.MultipleChoice.Cardinality;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import com.google.android.gnd.ui.editobservation.MultiSelectDialogFactory;
import com.google.android.gnd.ui.editobservation.SingleSelectDialogFactory;
import java8.util.Optional;
import timber.log.Timber;

public class MultipleChoiceFieldView extends FieldView {

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

  public void onShowDialog(Field field) {
    Cardinality cardinality = field.getMultipleChoice().getCardinality();
    Optional<Response> currentResponse = getEditObservationViewModel().getResponse(field.getId());
    switch (cardinality) {
      case SELECT_MULTIPLE:
        new MultiSelectDialogFactory(getContext())
            .create(
                field,
                currentResponse,
                r -> getEditObservationViewModel().onResponseChanged(field, r))
            .show();
        break;
      case SELECT_ONE:
        new SingleSelectDialogFactory(getContext())
            .create(
                field,
                currentResponse,
                r -> getEditObservationViewModel().onResponseChanged(field, r))
            .show();
        break;
      default:
        Timber.e("Unknown cardinality: %s", cardinality);
        break;
    }
  }
}
