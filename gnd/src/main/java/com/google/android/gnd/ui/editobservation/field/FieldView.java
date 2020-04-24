package com.google.android.gnd.ui.editobservation.field;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.util.ViewUtil;

public abstract class FieldView extends FrameLayout {

  private final ViewModelFactory viewModelFactory;
  private final Field field;

  public FieldView(Context context, ViewModelFactory viewModelFactory, Field field) {
    super(context);
    this.viewModelFactory = viewModelFactory;
    this.field = field;

    ViewUtil.assignGeneratedId(this);
  }

  protected LayoutInflater getLayoutInflater() {
    return LayoutInflater.from(getContext());
  }
}
