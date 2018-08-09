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

package com.google.android.gnd.ui.common;

import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.ui.editrecord.MultipleChoiceFieldLayout;
import com.google.android.gnd.vo.Form.Field;
import com.google.android.gnd.vo.Record.Value;
import java8.util.function.Consumer;

public class BindingAdapters {

  private static final String TAG = BindingAdapters.class.getSimpleName();

  @BindingAdapter("android:text")
  public static void bindText(TextInputEditText view, Value value) {
    ViewDataBinding binding = findBinding(view);
    Field field = getField(binding);
    if (field == null) {
      // Binding update before attached to field.
      return;
    }
    String newText = value == null ? "" : value.getDetailsText(field);
    if (!view.getText().toString().equals(newText)) {
      view.setText(newText);
    }
  }

  private static ViewDataBinding findBinding(View view) {
    for (View v = view; v.getParent() instanceof View; v = (View) v.getParent()) {
      ViewDataBinding binding = DataBindingUtil.getBinding(v);
      if (binding != null) {
        return binding;
      }
    }
    return null;
  }

  private static Field getField(ViewDataBinding binding) {
    if (binding == null) {
      return null;
    } else if (binding instanceof TextInputFieldBinding) {
      return ((TextInputFieldBinding) binding).getField();
    } else if (binding instanceof MultipleChoiceInputFieldBinding) {
      return ((MultipleChoiceInputFieldBinding) binding).getField();
    } else {
      throw new IllegalArgumentException("Unknown binding type: " + binding.getClass());
    }
  }

  @BindingAdapter("textChangedListener")
  public static void bindTextWatcher(TextInputEditText editText, Consumer onTextChanged) {
    editText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // No-op.
          }

          @Override
          public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            onTextChanged.accept(charSequence.toString());
          }

          @Override
          public void afterTextChanged(Editable editable) {
            // No-op.
          }
        });
  }

  @BindingAdapter("errorText")
  public static void bindError(TextInputEditText view, @Nullable String newErrorText) {
    if (view.getError() == null) {
      if (newErrorText != null) {
        view.setError(newErrorText);
      }
    } else if (!view.getError().equals(newErrorText)) {
      view.setError(newErrorText);
    }
  }

  @BindingAdapter("onShowDialog")
  public static void setOnShowDialogListener(MultipleChoiceFieldLayout view, Runnable listener) {
    view.setOnShowDialogListener(listener);
  }
}
