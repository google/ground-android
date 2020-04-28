package com.google.android.gnd.ui.field;

import android.view.LayoutInflater;
import android.widget.FrameLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.util.ViewUtil;

public abstract class FieldView extends FrameLayout {

  protected final Field field;
  private final ViewModelFactory viewModelFactory;
  private final AbstractFragment fragment;
  private final boolean editMode;
  private final FieldViewModel viewModel;

  public FieldView(ViewModelFactory viewModelFactory, AbstractFragment fragment, Field field) {
    this(viewModelFactory, fragment, field, true);
  }

  public FieldView(
      ViewModelFactory viewModelFactory, AbstractFragment fragment, Field field, boolean editMode) {
    super(fragment.getContext());
    this.viewModelFactory = viewModelFactory;
    this.field = field;
    this.fragment = fragment;
    this.editMode = editMode;
    this.viewModel = viewModelFactory.get(fragment, FieldViewModel.class);
    ViewUtil.assignGeneratedId(this);
    onCreateView();
  }

  public FieldViewModel getViewModel() {
    return viewModel;
  }

  public boolean isEditMode() {
    return editMode;
  }

  protected LayoutInflater getLayoutInflater() {
    return LayoutInflater.from(getContext());
  }

  protected LifecycleOwner getLifecycleOwner() {
    return fragment.getViewLifecycleOwner();
  }

  protected <T extends ViewModel> T getViewModel(Class<T> modelClass) {
    return viewModelFactory.get(fragment, modelClass);
  }

  protected <T extends ViewModel> T createViewModel(Class<T> modelClass) {
    return viewModelFactory.create(modelClass);
  }

  /** Initialize layout. */
  public abstract void onCreateView();

  /** Free system resources (if needed). */
  public void onPause() {}
}
