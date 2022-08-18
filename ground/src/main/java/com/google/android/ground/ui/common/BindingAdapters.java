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

package com.google.android.ground.ui.common;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.BindingAdapter;
import com.google.android.gms.common.SignInButton;
import com.google.android.ground.R;
import com.google.android.ground.ui.editsubmission.MultipleChoiceTaskLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;
import java8.util.function.Consumer;

/**
 * Container for adapter methods defining custom data binding behavior. This class cannot be made
 * injectable, since binding adapters must be static.
 */
public class BindingAdapters {

  @BindingAdapter("src")
  public static void bindImageBitmap(ImageView imageView, Bitmap bitmap) {
    imageView.setImageBitmap(bitmap);
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

  @BindingAdapter("onShowDialog")
  public static void setOnShowDialogListener(MultipleChoiceTaskLayout view, Runnable listener) {
    view.setOnShowDialogListener(listener);
  }

  @BindingAdapter("imageUrl")
  public static void bindUri(ImageView view, String url) {
    Picasso.get().load(url).placeholder(R.drawable.ic_photo_grey_600_24dp).into(view);
  }

  @BindingAdapter("imageUri")
  public static void bindUri(ImageView view, Uri uri) {
    Picasso.get().load(uri).placeholder(R.drawable.ic_photo_grey_600_24dp).into(view);
  }

  @BindingAdapter("tint")
  public static void bindImageTint(ImageView imageView, int colorId) {
    if (colorId == 0) {
      // Workaround for default value from uninitialized LiveData.
      return;
    }
    int tint = ContextCompat.getColor(imageView.getContext(), colorId);
    ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(tint));
  }

  @BindingAdapter("visible")
  public static void bindVisible(View view, boolean visible) {
    view.setVisibility(visible ? View.VISIBLE : View.GONE);
  }
}
