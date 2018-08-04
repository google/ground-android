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
import com.google.android.gnd.vo.Form.Field;
import com.google.android.gnd.vo.Record;

public class TextInputViewHolder implements Editable {

  private TextInputFieldBinding binding;
  private final View view;

  @BindView(R.id.text_input_layout)
  TextInputLayout layout;

  @BindView(R.id.text_input_edit_text)
  TextInputEditText editText;

  private TextInputViewHolder(TextInputFieldBinding binding) {
    this.binding = binding;
    this.view = binding.getRoot();
  }

  public static TextInputViewHolder newInstance(
    EditRecordFragment fragment,
    EditRecordViewModel viewModel,
    ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    TextInputFieldBinding binding = TextInputFieldBinding.inflate(inflater, parent, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(fragment);
    TextInputViewHolder holder = new TextInputViewHolder(binding);
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
    layout.setHint(field.getLabel());
    binding.setKey(field.getId());
  }


  @OnFocusChange(R.id.text_input_edit_text)
  void onFocusChange(View target, boolean hasFocus) {
    if (!hasFocus) {
      binding.getViewModel().validate(binding.getKey());
      // TODO: Move validation to ViewModel.
      // updateValidationMessage();
    }
  }
}
