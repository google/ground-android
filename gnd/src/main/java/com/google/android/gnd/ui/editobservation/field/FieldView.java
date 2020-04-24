package com.google.android.gnd.ui.editobservation.field;

import android.view.LayoutInflater;
import android.widget.FrameLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationViewModel;
import com.google.android.gnd.ui.util.ViewUtil;

public abstract class FieldView extends FrameLayout {

  protected final Field field;
  private final ViewModelFactory viewModelFactory;
  private final AbstractFragment fragment;

  public FieldView(ViewModelFactory viewModelFactory, Field field, AbstractFragment fragment) {
    super(fragment.getContext());
    this.viewModelFactory = viewModelFactory;
    this.field = field;
    this.fragment = fragment;

    ViewUtil.assignGeneratedId(this);
  }

  protected LayoutInflater getLayoutInflater() {
    return LayoutInflater.from(getContext());
  }

  protected LifecycleOwner getLifecycleOwner() {
    return fragment.getViewLifecycleOwner();
  }

  protected EditObservationViewModel getEditObservationViewModel() {
    return getViewModel(EditObservationViewModel.class);
  }

  protected <T extends ViewModel> T getViewModel(Class<T> modelClass) {
    return viewModelFactory.get(fragment, modelClass);
  }

  protected <T extends ViewModel> T createViewModel(Class<T> modelClass) {
    return viewModelFactory.create(modelClass);
  }
}
