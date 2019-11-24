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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import com.google.android.gnd.R;
import com.google.android.material.textfield.TextInputEditText;

public class MultipleChoiceFieldLayout extends FrameLayout {

  @BindView(R.id.multiple_choice_input_edit_text)
  TextInputEditText editText;

  private Runnable showDialogListener;

  public MultipleChoiceFieldLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  public void setOnShowDialogListener(Runnable showDialogListener) {
    this.showDialogListener = showDialogListener;
  }

  @OnFocusChange(R.id.multiple_choice_input_edit_text)
  void onFocusChange(View target, boolean hasFocus) {
    if (hasFocus) {
      showDialogListener.run();
    }
  }

  @OnClick(R.id.multiple_choice_tap_target)
  void onClick() {
    if (editText.isFocused()) {
      showDialogListener.run();
    } else {
      editText.requestFocus();
    }
  }
}
