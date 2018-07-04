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

import static com.google.android.gnd.ui.util.ViewUtil.getColorForStates;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.CREATE;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.DELETE;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.NO_CHANGE;
import static com.google.android.gnd.vo.PlaceUpdate.Operation.UPDATE;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnFocusChange;
import com.google.android.gnd.R;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Record;
import java8.util.Optional;

public class TextFieldView extends ConstraintLayout implements Editable {
  @BindView(R.id.text_field_label)
  TextView labelText;

  @BindView(R.id.text_field_input)
  EditText editText;

  @BindView(R.id.error_text)
  TextView errorMessageTextView;

  private String elementId;
  private Optional<Record.Value> originalValue;
  private int focusedLabelColor;
  private int defaultLabelColor;
  private boolean required;

  public TextFieldView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    ButterKnife.bind(this);

    focusedLabelColor = getColorForStates(labelText, TextView.FOCUSED_STATE_SET);
    defaultLabelColor = getColorForStates(labelText, TextView.ENABLED_STATE_SET);
  }

  @OnFocusChange(R.id.text_field_input)
  void onFocusChange(View target, boolean hasFocus) {
    if (!editText.isEnabled()) {
      return;
    }
    if (hasFocus) {
      labelText.setTextColor(focusedLabelColor);
    } else {
      labelText.setTextColor(defaultLabelColor);
      updateValidationMessage();
    }
  }

  @Override
  public boolean isValid() {
    return !isMissing();
  }

  boolean isMissing() {
    return required && !getCurrentValue().isPresent();
  }

  @Override
  public void updateValidationMessage() {
    if (isMissing()) {
      errorMessageTextView.setText(R.string.required_field);
      errorMessageTextView.setVisibility(VISIBLE);
    } else {
      errorMessageTextView.setVisibility(GONE);
    }
  }

  @Override
  public void init(Form.Field field, Record record) {
    init(field.getId(), field.getLabel(), record.getValue(field.getId()), field.isRequired());
  }

  private void init(
    String elementId, String label, Optional<Record.Value> value, boolean required) {
    this.elementId = elementId;
    originalValue = value;
    labelText.setText(label);
    editText.setText(value.map(Record.Value::getText).orElse(""));
    this.required = required;
  }

  private Optional<Record.Value> getCurrentValue() {
    String text = getText().trim();
    if (text.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(Record.Value.ofText(text));
  }

  @NonNull
  private String getText() {
    return editText.getText().toString().trim();
  }

  @Override
  public boolean isModified() {
    return !getText().equals(originalValue);
  }

  @Override
  public ValueUpdate getUpdate() {
    ValueUpdate.Builder update = ValueUpdate.newBuilder();
    update.setElementId(elementId);
    Optional<Record.Value> currentValue = getCurrentValue();
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
}
