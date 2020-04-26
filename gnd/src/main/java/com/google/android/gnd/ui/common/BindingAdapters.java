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

import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import com.google.android.gms.common.SignInButton;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.editobservation.field.MultipleChoiceFieldLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;
import java8.util.function.Consumer;

/**
 * Container for adapter methods defining custom data binding behavior. This class cannot be made
 * injectable, since binding adapters must be static.
 */
public class BindingAdapters {

  @BindingAdapter("android:text")
  public static void bindText(TextInputEditText view, Response response) {
    ViewDataBinding binding = findBinding(view);
    Field field = getField(binding);
    if (field == null) {
      // Binding update before attached to field.
      return;
    }
    String newText = response == null ? "" : response.getDetailsText(field);
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

  @BindingAdapter("onClick")
  public static void bindGoogleSignOnButtonClick(
      SignInButton button, View.OnClickListener onClickCallback) {
    button.setOnClickListener(onClickCallback);
  }

  @BindingAdapter("onTextChanged")
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

  @BindingAdapter("onFocusChange")
  public static void setOnFocusChangeListener(
      View view, final View.OnFocusChangeListener listener) {
    view.setOnFocusChangeListener(listener);
  }

  @BindingAdapter("onShowDialog")
  public static void setOnShowDialogListener(MultipleChoiceFieldLayout view, Runnable listener) {
    view.setOnShowDialogListener(listener);
  }

  @BindingAdapter("imageUri")
  public static void bindUri(ImageView view, Uri uri) {
    Picasso.get().load(uri).placeholder(R.drawable.ic_photo_grey_600_24dp).into(view);
  }
}
