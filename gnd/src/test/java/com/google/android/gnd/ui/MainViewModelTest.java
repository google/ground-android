/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.repository.TermsOfServiceRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.SchedulersModule;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.auth.AuthenticationModule;
import com.google.android.gnd.system.auth.SignInState;
import com.google.android.gnd.system.auth.SignInState.State;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.signin.SignInFragmentDirections;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@UninstallModules({SchedulersModule.class,
    LocalDatabaseModule.class})
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class MainViewModelTest {

  private static final TermsOfService TOS = TermsOfService.builder().setId("1")
      .setText("Test Terms").build();

  private static final Observable<SignInState> SIGN_IN_STATE =
      Observable.just(new SignInState(State.SIGNED_IN));

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();
  @Rule
  public HiltAndroidRule hiltRule = new HiltAndroidRule(this);
  @Rule
  public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
  public MainViewModel mainViewModel;
  RemoteDataStore mockRemoteDataStore;
  AuthenticationManager authenticationManager;
  @Mock
  ProjectRepository mockProjectRepository;
  @Mock
  FeatureRepository mockFeatureRepository;
  @Mock
  UserRepository mockUserRepository;
  @Mock
  TermsOfServiceRepository mockTermsOfServiceRepository;
  @Inject
  Navigator navigator;
  @Inject
  LocalValueStore localValueStore;
  @Inject
  Schedulers schedulers;

  @Before
  public void setup() {
    hiltRule.inject();
    mockProjectRepository = new ProjectRepository(null, mockRemoteDataStore, null, localValueStore);
    mockRemoteDataStore = mock(RemoteDataStore.class);
    authenticationManager = mock(AuthenticationManager.class);

    when(authenticationManager.getSignInState()).thenReturn(SIGN_IN_STATE);
    // returning empty terms of service by default.
    when(mockRemoteDataStore.loadTermsOfService()).thenReturn(Maybe.empty());
    mockTermsOfServiceRepository =
        new TermsOfServiceRepository(
            mockRemoteDataStore,
            localValueStore);
    mainViewModel = new MainViewModel(mockProjectRepository, mockFeatureRepository,
        mockUserRepository,
        mockTermsOfServiceRepository, navigator, authenticationManager, null, schedulers);
  }

  @Test
  public void testSignedIn_emptyTermsOfService() {
    mainViewModel.onSignedIn().test().assertValue(HomeScreenFragmentDirections.showHomeScreen());
  }

  @Test
  public void testSignedIn_filledTermsOfService() {
    when(mockRemoteDataStore.loadTermsOfService()).thenReturn(Maybe.just(TOS));
    mainViewModel.onSignedIn().test().assertValue(SignInFragmentDirections.showTermsOfService()
        .setTermsOfServiceText(TOS.getText()));
  }

}

