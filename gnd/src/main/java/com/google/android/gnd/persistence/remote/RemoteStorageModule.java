package com.google.android.gnd.persistence.remote;

import com.google.android.gnd.Config;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.android.gnd.persistence.remote.firestore.FirestoreDataStore;
import com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager;
import com.google.android.gnd.persistence.remote.firestore.FirestoreUuidGenerator;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
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
@Module
abstract class RemoteStorageModule {

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

  /** Returns a reference to the default Storage bucket. */
  @Provides
  @Singleton
  static StorageReference firebaseStorageReference() {
    return FirebaseStorage.getInstance().getReference();
  }

  /** Provides the Firestore implementation of remote data store. */
  @Binds
  @Singleton
  abstract RemoteDataStore remoteDataStore(FirestoreDataStore ds);

  /** Provides the Firestore implementation of offline unique id generation. */
  @Binds
  @Singleton
  abstract OfflineUuidGenerator offlineUuidGenerator(FirestoreUuidGenerator uuidGenerator);

  /** Provides the Firestore implementation of remote storage manager. */
  @Binds
  @Singleton
  abstract RemoteStorageManager remoteStorageManager(FirestoreStorageManager fsm);
}
