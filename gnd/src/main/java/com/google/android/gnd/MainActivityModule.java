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
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.inject.FragmentScoped;
import com.google.android.gnd.ui.editrecord.EditRecordFragment;
import com.google.android.gnd.ui.editrecord.EditRecordModule;
import com.google.android.gnd.ui.home.AddPlaceDialogFragment;
import com.google.android.gnd.ui.home.HomeScreenFragment;
import com.google.android.gnd.ui.home.HomeScreenModule;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerFragment;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerModule;
import com.google.android.gnd.ui.home.placesheet.PlaceSheetFragment;
import com.google.android.gnd.ui.home.placesheet.PlaceSheetModule;
import com.google.android.gnd.ui.home.placesheet.RecordListFragment;
import com.google.android.gnd.ui.projectselector.ProjectSelectorDialogFragment;
import com.google.android.gnd.ui.recorddetails.RecordDetailsFragment;
import com.google.android.gnd.ui.recorddetails.RecordDetailsModule;
import com.google.android.gnd.ui.signin.SignInFragment;
import com.google.android.gnd.ui.signin.SignInModule;
import com.google.android.gnd.ui.startup.StartupFragment;
import com.google.android.gnd.ui.startup.StartupModule;
import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/** Provides dependencies to {@link MainActivity} and fragments. */
@Module
public abstract class MainActivityModule {
  /**
   * This provides the activity required to inject the fragment manager into {@link MainActivity}.
   */
  @Binds
  @ActivityScoped
  abstract AppCompatActivity appCompatActivity(MainActivity mainActivity);

  @FragmentScoped
  @ContributesAndroidInjector(modules = StartupModule.class)
  abstract StartupFragment startupFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = SignInModule.class)
  abstract SignInFragment signInFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = HomeScreenModule.class)
  abstract HomeScreenFragment homeScreenFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = MapContainerModule.class)
  abstract MapContainerFragment mapContainerFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = HomeScreenModule.class)
  abstract ProjectSelectorDialogFragment projectSelectorDialogFragment();

  @FragmentScoped
  @ContributesAndroidInjector(modules = MapContainerModule.class)
  abstract AddPlaceDialogFragment addPlaceDialogFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = PlaceSheetModule.class)
  abstract PlaceSheetFragment placeSheetFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = PlaceSheetModule.class)
  abstract RecordListFragment recordListFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = RecordDetailsModule.class)
  abstract RecordDetailsFragment recordDetailsFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = EditRecordModule.class)
  abstract EditRecordFragment editRecordFragmentInjector();
}
