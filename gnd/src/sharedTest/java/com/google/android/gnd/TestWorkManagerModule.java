package com.google.android.gnd;

import androidx.work.WorkManager;
import com.google.android.gnd.persistence.sync.FakeWorkManager;
import com.google.android.gnd.persistence.sync.WorkManagerModule;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;
import javax.inject.Singleton;

@Module
@TestInstallIn(components = SingletonComponent.class, replaces = WorkManagerModule.class)
abstract class TestWorkManagerModule {

  @Provides
  @Singleton
  static WorkManager provideWorkManager() {
    return new FakeWorkManager();
  }
}
