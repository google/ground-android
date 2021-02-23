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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Geocoder;
import androidx.work.WorkManager;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gnd.ui.common.ViewModelModule;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Singleton;

@InstallIn(ApplicationComponent.class)
@Module(includes = {ViewModelModule.class})
abstract class GndApplicationModule {

  @Provides
  @Singleton
  static GoogleApiAvailability googleApiAvailability() {
    return GoogleApiAvailability.getInstance();
  }

  @Provides
  @Singleton
  static WorkManager workManager() {
    return WorkManager.getInstance();
  }

  @Provides
  @Singleton
  static SharedPreferences provideSharedPreferences(Application application) {
    return application
        .getApplicationContext()
        .getSharedPreferences(Config.SHARED_PREFS_NAME, Config.SHARED_PREFS_MODE);
  }

  @Provides
  @Singleton
  static Geocoder provideGeocoder(@ApplicationContext Context context) {
    return new Geocoder(context);
  }

  @Provides
  static Resources provideResources(@ApplicationContext Context context) {
    return context.getResources();
  }
}
