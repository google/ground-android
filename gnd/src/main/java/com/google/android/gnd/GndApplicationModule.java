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
import android.content.SharedPreferences;
import androidx.work.WorkManager;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.android.gnd.persistence.remote.firestore.FirestoreDataStore;
import com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager;
import com.google.android.gnd.persistence.remote.firestore.FirestoreUuidGenerator;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.auth.GoogleAuthenticationManager;
import com.google.android.gnd.ui.common.ViewModelModule;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
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
  static SharedPreferences sharedPreferences(Application application) {
    return application
        .getApplicationContext()
        .getSharedPreferences(Config.SHARED_PREFS_NAME, Config.SHARED_PREFS_MODE);
  }
}
