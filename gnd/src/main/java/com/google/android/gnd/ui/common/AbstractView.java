/*
 * Copyright 2021 Google LLC
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

import android.content.Context;
import android.content.ContextWrapper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;
import javax.inject.Inject;

public abstract class AbstractView extends FrameLayout {

  @Inject protected ViewModelFactory viewModelFactory;

  public AbstractView(@NonNull Context context) {
    super(context);
  }

  protected FragmentActivity getActivity() {
    Context context = getContext();
    while (context instanceof ContextWrapper) {
      if (context instanceof FragmentActivity) {
        return (FragmentActivity) context;
      }
      context = ((ContextWrapper) context).getBaseContext();
    }
    throw new IllegalStateException("View is not contained in FragmentActivity");
  }

  protected <T extends ViewModel> T getViewModel(Class<T> modelClass) {
    return viewModelFactory.get(getActivity(), modelClass);
  }

  protected ViewDataBinding inflate(@LayoutRes int layoutId) {
    return DataBindingUtil.inflate(
        (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE),
        layoutId,
        this,
        true);
  }
}
