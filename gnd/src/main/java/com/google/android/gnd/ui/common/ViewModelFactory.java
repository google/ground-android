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

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Generic factory class to create ViewModels using Dagger 2 injections, working around the fact
 * that we cannot inject view models directly. Based on Android Architecture Components example:
 * https://github.com/googlesamples/android-architecture-components/blob/b1a194c1ae267258cd002e2e1c102df7180be473/GithubBrowserSample/app/src/main/java/com/android/example/github/viewmodel/GithubViewModelFactory.java
 */
public class ViewModelFactory implements ViewModelProvider.Factory {
  private final Map<Class<? extends ViewModel>, Provider<ViewModel>> creators;

  @Inject
  public ViewModelFactory(Map<Class<? extends ViewModel>, Provider<ViewModel>> creators) {
    this.creators = creators;
  }

  @Override
  public <T extends ViewModel> T create(Class<T> modelClass) {
    Provider<? extends ViewModel> creator = creators.get(modelClass);
    if (creator == null) {
      throw new IllegalArgumentException("Unknown model class " + modelClass);
    }
    return (T) creator.get();
  }

  public <T extends ViewModel> T get(Fragment fragment, Class<T> modelClass) {
    if (modelClass.getAnnotation(ActivityScope.class) == null) {
      return ViewModelProviders.of(fragment, this).get(modelClass);
    } else {
      return get(fragment.getActivity(), modelClass);
    }
  }

  public <T extends ViewModel> T get(FragmentActivity activity, Class<T> modelClass) {
    return ViewModelProviders.of(activity, this).get(modelClass);
  }
}
