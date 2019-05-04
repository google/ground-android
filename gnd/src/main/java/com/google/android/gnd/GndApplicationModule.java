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

import android.app.Application;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.service.RemoteDataStore;
import com.google.android.gnd.service.firestore.FirestoreDataStore;
import com.google.android.gnd.ui.common.ViewModelModule;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import javax.inject.Singleton;

@Module(includes = {AndroidSupportInjectionModule.class, ViewModelModule.class})
abstract class GndApplicationModule {

  /** Causes Dagger Android to generate a sub-component for the MainActivity. */
  @ActivityScoped
  @ContributesAndroidInjector(modules = MainActivityModule.class)
  abstract MainActivity mainActivityInjector();

  /** Provides the Firestore implementation of our remote data store. */
  @Binds
  @Singleton
  abstract RemoteDataStore remoteDataStore(FirestoreDataStore ds);

  /** Provides the Firestore implementation of offline unique id generation. */
  @Binds
  @Singleton
  abstract OfflineUuidGenerator offlineUuidGenerator(FirestoreDataStore ds);

  @Binds
  @Singleton
  abstract Application application(GndApplication app);

  @Provides
  @Singleton
  static GoogleApiAvailability googleApiAvailability() {
    return GoogleApiAvailability.getInstance();
  }
}
