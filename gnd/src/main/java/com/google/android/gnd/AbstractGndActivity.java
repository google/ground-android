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

package com.google.android.gnd;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;

/**
 * Base class for all app activities supporting injection of Fragments.
 */
public class AbstractGndActivity extends AppCompatActivity implements HasSupportFragmentInjector {

  @Inject
  @Named(AbstractGndActivityModule.ACTIVITY_FRAGMENT_MANAGER)
  FragmentManager fragmentManager;

  @Inject
  DispatchingAndroidInjector<Fragment> fragmentInjector;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  public final AndroidInjector<Fragment> supportFragmentInjector() {
    return fragmentInjector;
  }

  protected final void addFragment(@IdRes int containerViewId, Fragment fragment) {
    fragmentManager.beginTransaction()
                   .add(containerViewId, fragment)
                   .commit();
  }
}
