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

package com.google.android.gnd.repository;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.rx.SchedulersModule;
import com.google.android.gnd.system.auth.AuthenticationManager;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@UninstallModules({SchedulersModule.class, LocalDatabaseModule.class})
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class FeatureRepositoryTest {

  @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  // This rule makes sure that Room executes all the database operations instantly.
  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Inject LocalDataStore localDataStore;
  @Mock RemoteDataStore remoteDataStore;
  @Mock ProjectRepository projectRepository;
  @Mock DataSyncWorkManager dataSyncWorkManager;
  @Mock AuthenticationManager authenticationManager;

  private FeatureRepository featureRepository;

  @Before
  public void setUp() {
    hiltRule.inject();
    featureRepository =
        new FeatureRepository(
            localDataStore,
            remoteDataStore,
            projectRepository,
            dataSyncWorkManager,
            authenticationManager);
  }
}
