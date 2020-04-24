package com.google.android.gnd.ui.editobservation.field;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import com.google.android.gnd.ui.util.ViewUtil;

public abstract class FieldView extends FrameLayout {

  public FieldView(Context context) {
    super(context);
    ViewUtil.assignGeneratedId(this);
  }

  protected LayoutInflater getLayoutInflater() {
    return LayoutInflater.from(getContext());
  }
}
