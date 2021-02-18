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

import static com.google.android.gnd.util.Debug.logLifecycleEvent;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import javax.inject.Inject;

public abstract class AbstractDialogFragment extends AppCompatDialogFragment {

  @Inject ViewModelFactory viewModelFactory;
  @Inject EphemeralPopups popups;

  /**
   * Uses {@link ViewModelFactory} to obtain an instance of the view model of the specified class.
   */
  protected <T extends ViewModel> T getViewModel(Class<T> modelClass) {
    return viewModelFactory.get(this, modelClass);
  }

  @Override
  public void onAttach(Context context) {
    logLifecycleEvent(this);
    super.onAttach(context);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    logLifecycleEvent(this);
    super.onCreate(savedInstanceState);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    logLifecycleEvent(this);
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    logLifecycleEvent(this);
    return super.onCreateDialog(savedInstanceState);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    logLifecycleEvent(this);
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onAttachFragment(Fragment childFragment) {
    logLifecycleEvent(this);
    super.onAttachFragment(childFragment);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    logLifecycleEvent(this);
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    logLifecycleEvent(this);
    super.onViewStateRestored(savedInstanceState);
  }

  @Override
  public void onStart() {
    logLifecycleEvent(this);
    super.onStart();
  }

  @Override
  public void onResume() {
    logLifecycleEvent(this);
    super.onResume();
  }

  @Override
  public void onPause() {
    logLifecycleEvent(this);
    super.onPause();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    logLifecycleEvent(this);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onStop() {
    logLifecycleEvent(this);
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    logLifecycleEvent(this);
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    logLifecycleEvent(this);
    super.onDestroy();
  }

  @Override
  public void onDetach() {
    logLifecycleEvent(this);
    super.onDetach();
  }

  protected Dialog fail(String message) {
    popups.showError(message);
    return new AlertDialog.Builder(getContext()).create();
  }
}
