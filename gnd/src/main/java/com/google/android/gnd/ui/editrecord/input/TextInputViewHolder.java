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

import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnFocusChange;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.system.DeviceCapabilities;
import com.google.android.gnd.ui.editrecord.EditRecordFragment;
import com.google.android.gnd.ui.editrecord.EditRecordViewModel;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.vo.Record;
import java8.util.Optional;

public class TextInputViewHolder implements Editable {
  private static TextInputFieldBinding binding;
  private final View view;

  @BindView(R.id.text_input_layout)
  TextInputLayout layout;

  @BindView(R.id.text_input_edit_text)
  TextInputEditText editText;

  private Form.Field field;
  private Optional<Record.Value> originalValue;

  private TextInputViewHolder(View view) {
    this.view = view;
  }

  public static TextInputViewHolder newInstance(
    EditRecordFragment fragment,
    EditRecordViewModel viewModel,
    ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    binding = TextInputFieldBinding.inflate(inflater, parent, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(fragment);
    TextInputViewHolder holder = new TextInputViewHolder(binding.getRoot());
    ButterKnife.bind(holder, binding.getRoot());
    if (DeviceCapabilities.isGenerateViewIdSupported()) {
      holder.editText.setId(View.generateViewId());
    }
    return holder;
  }

  public View getView() {
    return view;
  }

  @Override
  public void init(Form.Field field, Record record) {
    this.field = field;
    this.originalValue = record.getValue(field.getId());
    layout.setHint(field.getLabel());
    binding.setKey(field.getId());
  }

  @Override
  public boolean isModified() {
    return false;
  }


  @Override
  public ValueUpdate getUpdate() {
    ValueUpdate.Builder update = ValueUpdate.newBuilder();
    update.setElementId(field.getId());
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

  @NonNull
  private String getText() {
    return editText.getText().toString().trim();
  }

  private Optional<Record.Value> getCurrentValue() {
    String text = getText().trim();
    if (text.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(Record.Value.ofText(text));
  }

  @Override
  public boolean isValid() {
    return !isMissingRequired();
  }

  private boolean isMissingRequired() {
    return field.isRequired() && !getCurrentValue().isPresent();
  }

  @OnFocusChange(R.id.text_input_edit_text)
  void onFocusChange(View target, boolean hasFocus) {
    if (!hasFocus) {
      // TODO: Move validation to ViewModel.
      updateValidationMessage();
    }
  }

  @Override
  public void updateValidationMessage() {
    if (isMissingRequired()) {
      editText.setError(view.getResources().getString(R.string.required_field));
    } else {
      editText.setError(null);
    }
  }
}
