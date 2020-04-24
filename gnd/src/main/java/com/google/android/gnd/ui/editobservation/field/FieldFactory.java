package com.google.android.gnd.ui.editobservation.field;

import androidx.annotation.Nullable;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import timber.log.Timber;

public class FieldFactory {

  private final EditObservationFragment editObservationFragment;
  private final ViewModelFactory viewModelFactory;

  public FieldFactory(
      EditObservationFragment editObservationFragment, ViewModelFactory viewModelFactory) {
    this.editObservationFragment = editObservationFragment;
    this.viewModelFactory = viewModelFactory;
  }

  @Nullable
  public FieldView addField(Field field) {
    FieldView fieldView = null;
    switch (field.getType()) {
      case TEXT:
        fieldView = new TextFieldView(viewModelFactory, editObservationFragment, field);
        break;
      case MULTIPLE_CHOICE:
        fieldView =
            new MultipleChoiceFieldView(viewModelFactory, editObservationFragment, field);
        break;
      case PHOTO:
        fieldView = new PhotoFieldView(viewModelFactory, editObservationFragment, field);
        break;
      default:
        Timber.w("Unimplemented field type: %s", field.getType());
        break;
    }
    return fieldView;
  }
}
