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

package com.google.android.gnd.ui.editobservation;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gnd.R;
import com.google.android.material.textfield.TextInputEditText;

public class MultipleChoiceFieldLayout extends FrameLayout {

  @Nullable private Runnable showDialogListener;

  public MultipleChoiceFieldLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public void setOnShowDialogListener(Runnable showDialogListener) {
    this.showDialogListener = showDialogListener;
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    TextInputEditText editText = findViewById(R.id.multiple_choice_input_edit_text);

    editText.setOnFocusChangeListener(
        (v, hasFocus) -> {
          if (hasFocus) {
            checkNotNull(showDialogListener, "showDialogListener must be not be null");
            showDialogListener.run();
          }
        });

    findViewById(R.id.multiple_choice_tap_target)
        .setOnClickListener(
            v -> {
              if (editText.isFocused()) {
                checkNotNull(showDialogListener, "showDialogListener must be not be null");
                showDialogListener.run();
              } else {
                editText.requestFocus();
              }
            });
  }
}
