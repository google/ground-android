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

/*
package com.google.android.gnd;


import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;

*/
/** Provides dependencies to {@link MainActivity} and fragments. *//*

@InstallIn(ActivityComponent.class)
@Module
public abstract class MainActivityModule {
  */
/**
   * This provides the activity required to inject the fragment manager into {@link MainActivity}.
   *//*

*/
/*
  @Binds
  @ActivityScoped
  abstract AppCompatActivity appCompatActivity(MainActivity mainActivity);
*//*


*/
/*
  @ContributesAndroidInjector(modules = StartupModule.class)
  abstract StartupFragment startupFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = SignInModule.class)
  abstract SignInFragment signInFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = HomeScreenModule.class)
  abstract HomeScreenFragment homeScreenFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = HomeScreenModule.class)
  abstract ProjectSelectorDialogFragment projectSelectorDialogFragment();
*//*


*/
/*
  @ContributesAndroidInjector(modules = MapContainerModule.class)
  abstract MapContainerFragment mapContainerFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = MapContainerModule.class)
  abstract AddFeatureDialogFragment addFeatureDialogFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = FragmentManagerModule.class)
  abstract FeatureDetailsFragment featureDetailsFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = FragmentManagerModule.class)
  abstract ObservationListFragment observationListFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = ObservationDetailsModule.class)
  abstract ObservationDetailsFragment observationDetailsFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = EditObservationModule.class)
  abstract EditObservationFragment editObservationFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = OfflineAreaSelectorModule.class)
  abstract OfflineAreaSelectorFragment offlineAreaSelectorFragmentInjector();
*//*


*/
/*
  @ContributesAndroidInjector(modules = OfflineAreasModule.class)
  abstract OfflineAreasFragment offlineAreasFragmentInjector();
*//*

*/
/*
  @ContributesAndroidInjector(modules = OfflineAreaViewerModule.class)
  abstract OfflineAreaViewerFragment offlineAreaViewerFragmentInjector();*//*

}
*/
