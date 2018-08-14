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
import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/** Provides dependencies to {@link MainActivity}. */
@Module
public abstract class MainActivityModule {
  /**
   * This provides the activity required to inject the fragment manager into {@link MainActivity}.
   */
  @Binds
  @PerActivity
  abstract AppCompatActivity appCompatActivity(MainActivity mainActivity);

  // TODO: Merge Fragment and activities modules into one.
  // Discussion:
  // https://stackoverflow.com/questions/36206989/dagger-should-we-create-each-component-and-module-for-each-activity-fragment

  @PerFragment
  @ContributesAndroidInjector(modules = HomeScreenModule.class)
  abstract HomeScreenFragment homeScreenFragmentInjector();

  @PerFragment
  @ContributesAndroidInjector(modules = MapContainerModule.class)
  abstract MapContainerFragment mapContainerFragmentInjector();

  @PerFragment
  @ContributesAndroidInjector(modules = HomeScreenModule.class)
  abstract ProjectSelectorDialogFragment projectSelectorDialogFragment();

  @PerFragment
  @ContributesAndroidInjector(modules = MapContainerModule.class)
  abstract AddPlaceDialogFragment addPlaceDialogFragmentInjector();

  @PerFragment
  @ContributesAndroidInjector(modules = PlaceSheetModule.class)
  abstract PlaceSheetFragment placeSheetFragmentInjector();

  @PerFragment
  @ContributesAndroidInjector(modules = PlaceSheetModule.class)
  abstract RecordListFragment recordListFragmentInjector();

  @PerFragment
  @ContributesAndroidInjector(modules = RecordDetailsModule.class)
  abstract RecordDetailsFragment recordDetailsFragmentInjector();

  @PerFragment
  @ContributesAndroidInjector(modules = EditRecordModule.class)
  abstract EditRecordFragment editRecordFragmentInjector();
}
