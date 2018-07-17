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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    logLifecycleEvent("onAttach()");
    AndroidSupportInjection.inject(this);
    super.onAttach(context);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    logLifecycleEvent("onCreate()");
    super.onCreate(savedInstanceState);
  }

  @Nullable
  @Override
  public View onCreateView(
    @NonNull LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState) {
    logLifecycleEvent("onCreateView()");
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    logLifecycleEvent("onCreateDialog()");
    return super.onCreateDialog(savedInstanceState);
  }

  @Override
  public void onViewCreated(
    @NonNull View view, @Nullable Bundle savedInstanceState) {
    logLifecycleEvent("onViewCreated()");
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onAttachFragment(Fragment childFragment) {
    logLifecycleEvent("onAttachFragment()");
    super.onAttachFragment(childFragment);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    logLifecycleEvent("onActivityCreated()");
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    logLifecycleEvent("onViewStateRestored()");
    super.onViewStateRestored(savedInstanceState);
  }

  @Override
  public void onStart() {
    logLifecycleEvent("onStart()");
    super.onStart();
  }

  @Override
  public void onResume() {
    logLifecycleEvent("onResume()");
    super.onResume();
  }

  @Override
  public void onPause() {
    logLifecycleEvent("onPause()");
    super.onPause();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    logLifecycleEvent("onSaveInstanceState()");
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onStop() {
    logLifecycleEvent("onStop()");
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    logLifecycleEvent("onDestroyView()");
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    logLifecycleEvent("onDestroy()");
    super.onDestroy();
  }

  @Override
  public void onDetach() {
    logLifecycleEvent("onDetach()");
    super.onDetach();
  }

  protected Dialog fail(String message) {
    EphemeralPopups.showError(getContext(), message);
    return new AlertDialog.Builder(getContext()).create();
  }

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return childFragmentInjector;
  }

  private void logLifecycleEvent(String event) {
    Log.v(getClass().getSimpleName(), "Lifecycle event: " + event);
  }
}
