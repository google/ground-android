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
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.editobservation.EditObservationFragmentArgs;
import com.google.android.gnd.ui.util.ViewUtil;
import java8.util.Optional;

public abstract class FieldView extends FrameLayout {

  protected final Field field;
  private final AbstractFragment fragment;
  private final EditObservationFragmentArgs args;
  private final boolean editMode;
  private final FieldModel model;
  protected Optional<Response> currentResponse;

  public FieldView(
      AbstractFragment fragment,
      Field field,
      Optional<Response> response,
      EditObservationFragmentArgs args) {
    this(fragment, field, response, args, true);
  }

  public FieldView(
      AbstractFragment fragment,
      Field field,
      Optional<Response> response,
      EditObservationFragmentArgs args,
      boolean editMode) {
    super(fragment.getContext());
    this.field = field;
    this.fragment = fragment;
    this.args = args;
    this.editMode = editMode;

    model = new FieldModel(field, getContext().getResources());
    setResponse(response);

    ViewUtil.assignGeneratedId(this);
    onCreateView();
  }

  public EditObservationFragmentArgs getArgs() {
    return args;
  }

  public FieldModel getModel() {
    return model;
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

  /** Initialize layout. */
  public abstract void onCreateView();

  /** Free system resources (if needed). */
  public void onPause() {}

  public abstract Optional<Response> getResponse();

  public void setResponse(Optional<Response> response) {
    currentResponse = response;
    response.ifPresent(r -> model.setResponse(r.getDetailsText(field)));
  }

  public Field getField() {
    return field;
  }
}
