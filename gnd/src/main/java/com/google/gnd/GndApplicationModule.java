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

import com.google.gnd.inject.PerActivity;
import com.google.gnd.service.DataService;
import com.google.gnd.service.firestore.FirestoreDataService;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Module(includes = AndroidSupportInjectionModule.class)
abstract class GndApplicationModule {
  /**
   * Singleton annotation not required here but included for clarity.
   */
  @Binds
  @Singleton
  abstract GndApplication application(GndApplication app);

  @PerActivity
  @ContributesAndroidInjector(modules = MainActivityModule.class)
  abstract MainActivity mainActivityInjector();

  /**
   * Provide the Firestore implementation of our backend data service.
   * TODO: Make the implementation configurable via custom connectors to allow supporting other
   * backend datastores.
   */
  @Binds
  @Singleton
  abstract DataService dataService(FirestoreDataService ds);
}
