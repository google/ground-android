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

package com.google.gnd;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.google.gnd.inject.PerActivity;

import javax.inject.Named;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

/**
 * Provides dependencies of {@link AbstractGndActivity}. This must be included in all activity
 * modules, which in turn must provide the appropriate implementation of {@link AppCompatActivity}.
 */
@Module
public abstract class AbstractGndActivityModule {

  static final String
      ACTIVITY_FRAGMENT_MANAGER =
      "AbstractGndActivityModule.activityFragmentManager";

  /**
   * PerActivity annotation isn't required here since they are already unique, but is included for
   * clarity.
   */
  @Binds
  @PerActivity
  abstract Activity activity(AppCompatActivity appCompatActivity);

  @Binds
  @PerActivity
  abstract Context activityContext(Activity activity);

  @Provides
  @Named(ACTIVITY_FRAGMENT_MANAGER)
  @PerActivity
  static FragmentManager activityFragmentManager(AppCompatActivity activity) {
    return activity.getSupportFragmentManager();
  }
}
