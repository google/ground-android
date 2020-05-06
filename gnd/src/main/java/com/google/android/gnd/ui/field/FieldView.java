/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.ui.field;

import android.view.LayoutInflater;
import android.widget.FrameLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.util.ViewUtil;
import java8.util.Optional;

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

  public Optional<Response> getResponse() {
    return getViewModel().getResponse(field.getId());
  }

  public Field getField() {
    return field;
  }
}
