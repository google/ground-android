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
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import java8.util.function.Consumer;

public class BindingAdapters {
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
}
