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

package com.google.android.ground;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.ground.inject.ActivityScoped;
import com.google.android.ground.inject.FragmentScoped;
import com.google.android.ground.ui.editobservation.EditObservationFragment;
import com.google.android.ground.ui.editobservation.EditObservationModule;
import com.google.android.ground.ui.home.AddFeatureDialogFragment;
import com.google.android.ground.ui.home.HomeScreenFragment;
import com.google.android.ground.ui.home.HomeScreenModule;
import com.google.android.ground.ui.home.featuresheet.FeatureSheetFragment;
import com.google.android.ground.ui.home.featuresheet.FeatureSheetModule;
import com.google.android.ground.ui.home.featuresheet.ObservationListFragment;
import com.google.android.ground.ui.home.mapcontainer.MapContainerFragment;
import com.google.android.ground.ui.home.mapcontainer.MapContainerModule;
import com.google.android.ground.ui.observationdetails.ObservationDetailsFragment;
import com.google.android.ground.ui.observationdetails.ObservationDetailsModule;
import com.google.android.ground.ui.offlinearea.OfflineAreasFragment;
import com.google.android.ground.ui.offlinearea.OfflineAreasModule;
import com.google.android.ground.ui.offlinearea.selector.OfflineAreaSelectorFragment;
import com.google.android.ground.ui.offlinearea.selector.OfflineAreaSelectorModule;
import com.google.android.ground.ui.projectselector.ProjectSelectorDialogFragment;
import com.google.android.ground.ui.signin.SignInFragment;
import com.google.android.ground.ui.signin.SignInModule;
import com.google.android.ground.ui.startup.StartupFragment;
import com.google.android.ground.ui.startup.StartupModule;
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
  abstract AddFeatureDialogFragment addFeatureDialogFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = FeatureSheetModule.class)
  abstract FeatureSheetFragment featureSheetFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = FeatureSheetModule.class)
  abstract ObservationListFragment observationListFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = ObservationDetailsModule.class)
  abstract ObservationDetailsFragment observationDetailsFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = EditObservationModule.class)
  abstract EditObservationFragment editObservationFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = OfflineAreaSelectorModule.class)
  abstract OfflineAreaSelectorFragment offlineAreaSelectorFragmentInjector();

  @FragmentScoped
  @ContributesAndroidInjector(modules = OfflineAreasModule.class)
  abstract OfflineAreasFragment offlineAreasFragmentInjector();
}
