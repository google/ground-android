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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;
import javax.inject.Inject;

public abstract class GndFragment extends Fragment implements HasSupportFragmentInjector {
  /**
   * Keeps track of fields bound to views so that they can be set to null when the view is
   * destroyed, freeing up memory.
   */
  private Unbinder unbinder;

  @Inject DispatchingAndroidInjector<Fragment> childFragmentInjector;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    onCreateViewModel();
  }

  protected void onCreateViewModel() {
  }

  @Nullable
  @Override
  public View onCreateView(
    @NonNull LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState) {
    View view = onInflateView(inflater, container, savedInstanceState);
    onAddFragments();
    onObserveViewModel();
    return view;
  }

  protected View onInflateView(
    LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    throw new UnsupportedOperationException(
      "Subclasses much override either onCreateView or inflateView");
  }

  protected void onAddFragments() {
  }

  protected void onObserveViewModel() {
  }

  @Override
  public void onAttach(Context context) {
    AndroidSupportInjection.inject(this);
    super.onAttach(context);
  }

  @Override
  public void onViewStateRestored(Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    // Bind views here instead of onViewCreated to avoid invoking view listeners.
    unbinder = ButterKnife.bind(this, getView());
    onBindViews();
  }

  protected void onBindViews() {
  }

  @Override
  public void onDestroyView() {
    if (unbinder != null) {
      unbinder.unbind();
    }
    super.onDestroyView();
  }

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return childFragmentInjector;
  }

  protected final void addFragment(@IdRes int containerViewId, Fragment fragment) {
    getChildFragmentManager().beginTransaction().replace(containerViewId, fragment).commit();
  }
}
