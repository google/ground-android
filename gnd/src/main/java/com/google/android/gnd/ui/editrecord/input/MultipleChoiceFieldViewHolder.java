/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.ui.editrecord.input;

import static com.google.android.gnd.vo.PlaceUpdate.Operation.CREATE;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.DELETE;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.NO_CHANGE;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.UPDATE;
import static com.google.android.gnd.vo.Record.Value;

import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import com.google.android.gnd.R;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Form.MultipleChoice.Cardinality;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Record;
import java8.util.Optional;

public class MultipleChoiceFieldViewHolder implements Editable {
  private static final String TAG = MultipleChoiceFieldViewHolder.class.getSimpleName();

  private final View view;
  private final SingleSelectDialogFactory singleSelectDialogFactory;
  private final MultiSelectDialogFactory multiSelectDialogFactory;

  @BindView(R.id.multiple_choice_input_layout)
  TextInputLayout layout;

  @BindView(R.id.multiple_choice_input_edit_text)
  TextInputEditText editText;

  private Form.Field field;
  private Optional<Value> originalValue;
  private Optional<Value> currentValue;

  private MultipleChoiceFieldViewHolder(View view) {
    this.view = view;
    // TODO: Use AutoFactory and Inject?
    this.singleSelectDialogFactory = new SingleSelectDialogFactory(view.getContext());
    this.multiSelectDialogFactory = new MultiSelectDialogFactory(view.getContext());
  }

  public static MultipleChoiceFieldViewHolder newInstance(ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view = inflater.inflate(R.layout.multiple_choice_input_field, parent, false);
    MultipleChoiceFieldViewHolder holder = new MultipleChoiceFieldViewHolder(view);
    ButterKnife.bind(holder, view);
    return holder;
  }

  public View getView() {
    return view;
  }

  @Override
  public void init(Form.Field field, Record record) {
    this.field = field;
    this.originalValue = record.getValue(field.getId());
    this.currentValue = this.originalValue;
    layout.setHint(field.getLabel());
    onValueUpdate(this.currentValue);
  }

  private void onValueUpdate(Optional<Value> value) {
    currentValue = value;
    editText.setText(value.map(v -> v.getDetailsText(field)).orElse(""));
    updateValidationMessage();
  }

  @Override
  public boolean isModified() {
    return !getCurrentValue().equals(originalValue);
  }

  @Override
  public ValueUpdate getUpdate() {
    ValueUpdate.Builder update = ValueUpdate.newBuilder();
    update.setElementId(field.getId());
    Optional<Value> currentValue = getCurrentValue();
    if (currentValue.equals(originalValue)) {
      update.setOperation(NO_CHANGE);
    } else if (!currentValue.isPresent()) {
      update.setOperation(DELETE);
    } else if (originalValue.isPresent()) {
      update.setOperation(UPDATE);
      update.setValue(currentValue);
    } else {
      update.setOperation(CREATE);
      update.setValue(currentValue);
    }
    return update.build();
  }

  private Optional<Value> getCurrentValue() {
    return currentValue;
  }

  @Override
  public boolean isValid() {
    return !isMissingRequired();
  }

  private boolean isMissingRequired() {
    return field.isRequired() && !getCurrentValue().isPresent();
  }

  @OnFocusChange(R.id.multiple_choice_input_edit_text)
  void onFocusChange(View target, boolean hasFocus) {
    if (hasFocus) {
      editText.setEnabled(false);
      showDialog();
    } else {
      editText.setEnabled(true);
      updateValidationMessage();
    }
  }

  @OnClick(R.id.multiple_choice_tap_target)
  void onClick() {
    if (editText.isFocused()) {
      showDialog();
    } else {
      editText.requestFocus();
    }
  }

  private void showDialog() {
    Cardinality cardinality = field.getMultipleChoice().getCardinality();
    switch (cardinality) {
      case SELECT_MULTIPLE:
        multiSelectDialogFactory.create(field, currentValue, this::onValueUpdate).show();
        break;
      case SELECT_ONE:
        singleSelectDialogFactory.create(field, currentValue, this::onValueUpdate).show();
        break;
      default:
        Log.e(TAG, "Unknown cardinality: " + cardinality);
        return;
    }
  }

  @Override
  public void updateValidationMessage() {
    // TODO: Move validation to ViewModel.
    if (isMissingRequired()) {
      editText.setError(view.getResources().getString(R.string.required_field));
    } else {
      editText.setError(null);
    }
  }
}
