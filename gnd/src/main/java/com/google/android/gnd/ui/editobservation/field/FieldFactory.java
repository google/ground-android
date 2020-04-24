package com.google.android.gnd.ui.editobservation.field;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import com.google.android.gnd.ui.editobservation.EditObservationViewModel;
import timber.log.Timber;

public class FieldFactory {

  private final Context context;
  private final EditObservationViewModel viewModel;
  private final EditObservationFragment editObservationFragment;
  private final ViewModelFactory viewModelFactory;

  public FieldFactory(
      Context context,
      EditObservationViewModel viewModel,
      EditObservationFragment editObservationFragment,
      ViewModelFactory viewModelFactory) {
    this.context = context;
    this.viewModel = viewModel;
    this.editObservationFragment = editObservationFragment;
    this.viewModelFactory = viewModelFactory;
  }

  private LifecycleOwner getLifecycleOwner() {
    return editObservationFragment.getViewLifecycleOwner();
  }

  @Nullable
  public FieldView addField(Field field) {
    FieldView fieldView = null;
    switch (field.getType()) {
      case TEXT:
        fieldView = new TextFieldView(context, viewModel, field, getLifecycleOwner());
        break;
      case MULTIPLE_CHOICE:
        fieldView =
            new MultipleChoiceFieldView(
                context, editObservationFragment, viewModel, field, getLifecycleOwner());
        break;
      case PHOTO:
        fieldView =
            new PhotoFieldView(
                context,
                viewModelFactory,
                editObservationFragment,
                viewModel,
                field,
                getLifecycleOwner());
        break;
      default:
        Timber.w("Unimplemented field type: %s", field.getType());
        break;
    }
    return fieldView;
  }
}
