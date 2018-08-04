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
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.system.DeviceCapabilities;
import com.google.android.gnd.ui.editrecord.EditRecordFragment;
import com.google.android.gnd.ui.editrecord.EditRecordViewModel;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Form.Field;
import com.google.android.gnd.vo.Form.MultipleChoice.Cardinality;
import com.google.android.gnd.vo.Record;
import java8.util.Optional;

public class MultipleChoiceFieldViewHolder implements Editable {
  private static final String TAG = MultipleChoiceFieldViewHolder.class.getSimpleName();

  private final View view;
  private final SingleSelectDialogFactory singleSelectDialogFactory;
  private final MultiSelectDialogFactory multiSelectDialogFactory;
  private MultipleChoiceInputFieldBinding binding;

  @BindView(R.id.multiple_choice_input_layout)
  TextInputLayout layout;

  @BindView(R.id.multiple_choice_input_edit_text)
  TextInputEditText editText;

  private Form.Field field;

  private MultipleChoiceFieldViewHolder(MultipleChoiceInputFieldBinding binding) {
    this.binding = binding;
    this.view = binding.getRoot();
    // TODO: Use AutoFactory and Inject?
    this.singleSelectDialogFactory = new SingleSelectDialogFactory(view.getContext());
    this.multiSelectDialogFactory = new MultiSelectDialogFactory(view.getContext());
  }

  public static MultipleChoiceFieldViewHolder newInstance(
    EditRecordFragment fragment, EditRecordViewModel viewModel, ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    MultipleChoiceInputFieldBinding binding = MultipleChoiceInputFieldBinding
        .inflate(inflater, parent, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(fragment);
    MultipleChoiceFieldViewHolder holder = new MultipleChoiceFieldViewHolder(binding);
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
  public void init(Record record, Field field) {
    this.field = field;
    binding.setKey(field.getId());
    layout.setHint(field.getLabel());
  }

  private void onValueUpdate(Optional<Value> value) {
    binding.getViewModel().onValueChanged(binding.getKey(), value);
  }

  @OnFocusChange(R.id.multiple_choice_input_edit_text)
  void onFocusChange(View target, boolean hasFocus) {
    if (hasFocus) {
      editText.setEnabled(false);
      showDialog();
    } else {
      editText.setEnabled(true);
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
    Optional<Value> currentValue = binding.getViewModel().getValue(binding.getKey());
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
}
