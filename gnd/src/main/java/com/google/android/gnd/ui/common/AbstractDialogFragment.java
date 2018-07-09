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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerAppCompatDialogFragment;
import javax.inject.Inject;

public abstract class AbstractDialogFragment extends DaggerAppCompatDialogFragment {
  @Inject DispatchingAndroidInjector<Fragment> childFragmentInjector;

  public AbstractDialogFragment() {}

  @Override
  public void onAttach(Context context) {
    AndroidSupportInjection.inject(this);
    super.onAttach(context);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    obtainViewModels();
  }

  protected void obtainViewModels() {}

  protected Dialog fail(String message) {
    EphemeralPopups.showError(getContext(), message);
    return new AlertDialog.Builder(getContext()).create();
  }

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return childFragmentInjector;
  }
}
