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
import androidx.room.Room;
import androidx.work.WorkManager;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.persistence.local.room.LocalDatabase;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.android.gnd.persistence.remote.firestore.FirestoreDataStore;
import com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager;
import com.google.android.gnd.persistence.remote.firestore.FirestoreUuidGenerator;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.RxSchedulers;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.system.AuthenticationModule;
import com.google.android.gnd.ui.common.ViewModelModule;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import javax.inject.Singleton;

@Module(includes = {AndroidSupportInjectionModule.class, ViewModelModule.class})
abstract class GndApplicationModule {
  private static final String SHARED_PREFERENCES_NAME = "shared_prefs";

  /** Causes Dagger Android to generate a sub-component for the MainActivity. */
  @ActivityScoped
  @ContributesAndroidInjector(modules = {MainActivityModule.class, AuthenticationModule.class})
  abstract MainActivity mainActivityInjector();

  /** Causes Dagger Android to generate a sub-component for the SettingsActivity. */
  @ActivityScoped
  @ContributesAndroidInjector(modules = SettingsActivityModule.class)
  abstract SettingsActivity settingsActivityInjector();

  /** Provides the Firestore implementation of remote data store. */
  @Binds
  @Singleton
  abstract RemoteDataStore remoteDataStore(FirestoreDataStore ds);

  @Binds
  @Singleton
  abstract Schedulers schedulers(RxSchedulers rxSchedulers);

  /** Provides the Firestore implementation of offline unique id generation. */
  @Binds
  @Singleton
  abstract OfflineUuidGenerator offlineUuidGenerator(FirestoreUuidGenerator uuidGenerator);

  @Binds
  @Singleton
  abstract Application application(GndApplication app);

  @Binds
  abstract Context context(GndApplication application);

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
  static SharedPreferences sharedPreferences(GndApplication application) {
    return application
        .getApplicationContext()
        .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
  }

  @Provides
  static FirebaseFirestoreSettings firebaseFirestoreSettings() {
    return new FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(Config.FIRESTORE_PERSISTENCE_ENABLED)
        .build();
  }

  @Provides
  @Singleton
  static FirebaseFirestore firebaseFirestore(FirebaseFirestoreSettings settings) {
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    firestore.setFirestoreSettings(settings);
    FirebaseFirestore.setLoggingEnabled(Config.FIRESTORE_LOGGING_ENABLED);
    return firestore;
  }

  /** Provides the Firestore implementation of remote storage manager. */
  @Binds
  @Singleton
  abstract RemoteStorageManager remoteStorageManager(FirestoreStorageManager fsm);

  /** Returns a reference to the default Storage bucket. */
  @Provides
  @Singleton
  static StorageReference firebaseStorageReference() {
    return FirebaseStorage.getInstance().getReference();
  }

  @Provides
  @Singleton
  static LocalDatabase localDatabase(Context context) {
    return Room.databaseBuilder(context, LocalDatabase.class, Config.DB_NAME)
      // TODO(#128): Disable before official release.
      .fallbackToDestructiveMigration()
      .build();
  }
}
