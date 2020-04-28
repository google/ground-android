package com.google.android.gnd.ui.field;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;

public class FieldFactory {

  private final AbstractFragment fragment;
  private final ViewModelFactory viewModelFactory;

  public FieldFactory(AbstractFragment fragment, ViewModelFactory viewModelFactory) {
    this.fragment = fragment;
    this.viewModelFactory = viewModelFactory;
  }

  @NonNull
  public FieldView createFieldView(Field field) {
    FieldView fieldView;
    switch (field.getType()) {
      case TEXT:
        fieldView = new TextFieldView(viewModelFactory, fragment, field);
        break;
      case MULTIPLE_CHOICE:
        fieldView = new MultipleChoiceFieldView(viewModelFactory, fragment, field);
        break;
      case PHOTO:
        fieldView = new PhotoFieldView(viewModelFactory, fragment, field);
        break;
      default:
        throw new IllegalStateException("Unimplemented field type: " + field.getType());
    }
    return fieldView;
  }
}
