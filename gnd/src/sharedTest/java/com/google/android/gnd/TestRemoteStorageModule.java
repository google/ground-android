package com.google.android.gnd;

import com.google.android.gnd.persistence.remote.FakeRemoteDataStore;
import com.google.android.gnd.persistence.remote.FakeRemoteStorageManager;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.android.gnd.persistence.remote.RemoteStorageModule;
import com.google.android.gnd.persistence.uuid.FakeUuidGenerator;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;
import javax.inject.Singleton;

@Module
@TestInstallIn(components = SingletonComponent.class, replaces = RemoteStorageModule.class)
abstract class TestRemoteStorageModule {

  @Binds
  @Singleton
  abstract RemoteDataStore bindRemoteDataStore(FakeRemoteDataStore remoteDataStore);

  @Binds
  @Singleton
  abstract RemoteStorageManager bindRemoteStorageManager(
      FakeRemoteStorageManager remoteStorageManager);

  @Binds
  @Singleton
  abstract OfflineUuidGenerator offlineUuidGenerator(FakeUuidGenerator uuidGenerator);
}
