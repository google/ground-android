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

import static com.google.android.gnd.ui.util.ViewUtil.hideSoftInputFrom;
import static com.google.android.gnd.util.Debug.logLifecycleEvent;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;
import javax.inject.Inject;

public abstract class AbstractFragment extends Fragment implements HasSupportFragmentInjector {
  /**
   * Keeps track of fields bound to views so that they can be set to null when the view is
   * destroyed, freeing up memory.
   */
  private Unbinder unbinder;

  @Inject
  protected ViewModelFactory viewModelFactory;

  @Inject DispatchingAndroidInjector<Fragment> childFragmentInjector;

  protected <T extends ViewModel> T getViewModel(Class<T> modelClass) {
    return viewModelFactory.get(this, modelClass);
  }

  @Override
  public void onAttach(Context context) {
    logLifecycleEvent(this);
    AndroidSupportInjection.inject(this);
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
  public void onSaveInstanceState(@NonNull Bundle outState) {
    logLifecycleEvent(this);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    logLifecycleEvent(this);
    super.onViewCreated(view, savedInstanceState);
    unbinder = ButterKnife.bind(this, view);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
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
    hideSoftInputFrom(this);
  }

  @Override
  public void onPause() {
    logLifecycleEvent(this);
    super.onPause();
  }

  @Override
  public void onStop() {
    logLifecycleEvent(this);
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    logLifecycleEvent(this);
    if (unbinder != null) {
      unbinder.unbind();
    }
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

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return childFragmentInjector;
  }

  protected final void replaceFragment(@IdRes int containerViewId, Fragment fragment) {
    getChildFragmentManager().beginTransaction().replace(containerViewId, fragment).commit();
  }

  protected void saveChildFragment(Bundle outState, Fragment fragment, String key) {
    getChildFragmentManager().putFragment(outState, key, fragment);
  }

  protected <T> T restoreChildFragment(Bundle savedInstanceState, String key) {
    return (T) getChildFragmentManager().getFragment(savedInstanceState, key);
  }

  protected <T> T restoreChildFragment(Bundle savedInstanceState, Class<T> fragmentClass) {
    return (T) restoreChildFragment(savedInstanceState, fragmentClass.getName());
  }
}
