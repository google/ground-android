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

import android.support.v7.app.AppCompatActivity;
import com.google.android.gnd.inject.PerActivity;
import com.google.android.gnd.inject.PerFragment;
import com.google.android.gnd.ui.MainFragment;
import com.google.android.gnd.ui.MainFragmentModule;
import com.google.android.gnd.ui.ProjectSelectorDialogFragment;
import com.google.android.gnd.ui.common.GndActivity;
import com.google.android.gnd.ui.common.GndActivityModule;
import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/** Provides dependencies to {@link MainActivity}. */
@Module(includes = GndActivityModule.class)
public abstract class MainActivityModule {

  /**
   * This provides the activity required to inject the fragment manager into {@link GndActivity}.
   */
  @Binds
  @PerActivity
  abstract AppCompatActivity appCompatActivity(MainActivity mainActivity);

  @PerFragment
  @ContributesAndroidInjector(modules = MainFragmentModule.class)
  abstract MainFragment mainFragmentInjector();

  @PerFragment
  @ContributesAndroidInjector(modules = MainFragmentModule.class)
  abstract ProjectSelectorDialogFragment projectSelectorDialogFragment();
}
