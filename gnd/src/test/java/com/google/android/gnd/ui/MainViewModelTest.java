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

import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
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
import com.google.android.gnd.system.auth.SignInState;
import com.google.android.gnd.system.auth.SignInState.State;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.signin.SignInFragmentDirections;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import java8.util.Optional;
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

  private static final Field TEST_FIELD =
      Field.newBuilder()
          .setId("field id")
          .setIndex(1)
          .setLabel("field label")
          .setRequired(false)
          .setType(Field.Type.TEXT_FIELD)
          .build();

  private static final Form TEST_FORM =
      Form.newBuilder()
          .setId("form id")
          .setElements(ImmutableList.of(Element.ofField(TEST_FIELD)))
          .build();

  private static final Layer TEST_LAYER =
      Layer.newBuilder()
          .setId("layer id")
          .setName("heading title")
          .setDefaultStyle(Style.builder().setColor("000").build())
          .setForm(TEST_FORM)
          .build();

  private static final Project TEST_PROJECT =
      Project.newBuilder()
          .setId("project id")
          .setTitle("project 1")
          .setDescription("foo description")
          .putLayer("layer id", TEST_LAYER)
          .build();


  private static final Flowable<Optional<Project>> TEST_ACTIVE_PROJECT  =
      Flowable.just(Optional.of(TEST_PROJECT));

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();
  @Rule
  public HiltAndroidRule hiltRule = new HiltAndroidRule(this);
  @Rule
  public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  public MainViewModel mainViewModel;

  @Mock
  RemoteDataStore mockRemoteDataStore;

  @Mock
  AuthenticationManager mockAuthenticationManager;

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

    when(mockAuthenticationManager.getSignInState()).thenReturn(SIGN_IN_STATE);
    when(mockProjectRepository.getActiveProject()).thenReturn(TEST_ACTIVE_PROJECT);

    // returning empty terms of service by default.
    when(mockRemoteDataStore.loadTermsOfService()).thenReturn(Maybe.empty());
    mockProjectRepository = new ProjectRepository(null, null, mockRemoteDataStore, localValueStore);

    mockTermsOfServiceRepository =
        new TermsOfServiceRepository(
            mockRemoteDataStore,
            localValueStore);
    mainViewModel = new MainViewModel(mockProjectRepository, mockFeatureRepository,
        mockUserRepository,
        mockTermsOfServiceRepository, navigator, mockAuthenticationManager, null, schedulers);
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

