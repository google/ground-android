/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.persistence.remote

import com.google.android.ground.BuildConfig.EMULATOR_HOST
import com.google.android.ground.BuildConfig.FIRESTORE_EMULATOR_PORT
import com.google.android.ground.BuildConfig.USE_EMULATORS
import com.google.android.ground.persistence.remote.firebase.FirebaseStorageManager
import com.google.android.ground.persistence.remote.firebase.FirestoreDataStore
import com.google.android.ground.persistence.remote.firebase.FirestoreUuidGenerator
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class RemotePersistenceModule {
  /** Provides the Firestore implementation of remote data store. */
  @Binds @Singleton abstract fun remoteDataStore(ds: FirestoreDataStore): RemoteDataStore

  /** Provides the Firestore implementation of offline unique id generation. */
  @Binds
  @Singleton
  abstract fun offlineUuidGenerator(uuidGenerator: FirestoreUuidGenerator): OfflineUuidGenerator

  /** Provides the Firestore implementation of remote storage manager. */
  @Binds
  @Singleton
  abstract fun remoteStorageManager(fsm: FirebaseStorageManager): RemoteStorageManager

  companion object {
    @Provides
    fun firebaseFirestoreSettings(): FirebaseFirestoreSettings =
      with(FirebaseFirestoreSettings.Builder()) {
        if (USE_EMULATORS) {
          host = "$EMULATOR_HOST:$FIRESTORE_EMULATOR_PORT"
          isSslEnabled = false
        }
        isPersistenceEnabled = false
        build()
      }

    /** Returns a reference to the default Storage bucket. */
    @Provides
    @Singleton
    fun firebaseStorageReference(): StorageReference {
      return FirebaseStorage.getInstance().reference
    }

    @Provides @Singleton fun firebaseFunctions(): FirebaseFunctions = Firebase.functions
  }
}
