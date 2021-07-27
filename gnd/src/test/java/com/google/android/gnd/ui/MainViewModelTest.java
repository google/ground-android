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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.navigation.NavDirections;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.TestObservers;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.repository.TermsOfServiceRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.SchedulersModule;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.auth.SignInState;
import com.google.android.gnd.system.auth.SignInState.State;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.signin.SignInFragmentDirections;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@UninstallModules({SchedulersModule.class, LocalDatabaseModule.class})
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class MainViewModelTest {

  private static final TermsOfService TEST_TERMS_OF_SERVICE =
      TermsOfService.builder().setId("1").setText("Test Terms").build();

  private static final User TEST_USER = FakeData.TEST_USER;

  private static final Flowable<Optional<Project>> TEST_ACTIVE_PROJECT =
      Flowable.just(Optional.of(FakeData.TEST_PROJECT));

  @Rule public MockitoRule rule = MockitoJUnit.rule();
  @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);
  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Mock ProjectRepository mockProjectRepository;
  @Mock FeatureRepository mockFeatureRepository;
  @Mock UserRepository mockUserRepository;
  @Mock TermsOfServiceRepository mockTosRepository;
  @Mock EphemeralPopups mockPopups;
  @Mock Navigator mockNavigator;

  @Inject Schedulers schedulers;

  private FakeAuthenticationManager authenticationManager;
  private MainViewModel viewModel;

  @Before
  public void setup() {
    hiltRule.inject();

    // TODO: Add a test for syncFeatures
    when(mockProjectRepository.getActiveProject()).thenReturn(TEST_ACTIVE_PROJECT);

    authenticationManager = new FakeAuthenticationManager();
    viewModel =
        new MainViewModel(
            mockProjectRepository,
            mockFeatureRepository,
            mockUserRepository,
            mockTosRepository,
            mockNavigator,
            authenticationManager,
            mockPopups,
            schedulers);
  }

  private void assertProgressDialogVisible(boolean visible) {
    TestObservers.observeUntilFirstChange(viewModel.getSignInProgressDialogVisibility());
    assertThat(viewModel.getSignInProgressDialogVisibility().getValue()).isEqualTo(visible);
  }

  private void assertNavigate(NavDirections navDirections) {
    Mockito.verify(mockNavigator, times(1)).navigate(navDirections);
  }

  @Test
  public void testSignInStateChanged_onSignedOut() {
    authenticationManager.signOut();

    assertProgressDialogVisible(false);
    assertNavigate(SignInFragmentDirections.showSignInScreen());
    Mockito.verify(mockProjectRepository, times(1)).clearActiveProject();
    Mockito.verify(mockUserRepository, times(1)).clearUserPreferences();
    Mockito.verify(mockTosRepository, times(1)).setTermsOfServiceAccepted(false);
  }

  @Test
  public void testSignInStateChanged_onSigningIn() {
    authenticationManager.signingIn();

    assertProgressDialogVisible(true);
    Mockito.verify(mockNavigator, times(0)).navigate(any());
    Mockito.verify(mockTosRepository, times(1)).setTermsOfServiceAccepted(false);
  }

  @Test
  public void testSignInStateChanged_onSignedIn_whenTosAccepted() {
    when(mockTosRepository.isTermsOfServiceAccepted()).thenReturn(true);
    when(mockUserRepository.saveUser(any(User.class))).thenReturn(Completable.complete());

    authenticationManager.signIn();

    assertProgressDialogVisible(false);
    assertNavigate(HomeScreenFragmentDirections.showHomeScreen());
    Mockito.verify(mockUserRepository, times(1)).saveUser(TEST_USER);
    Mockito.verify(mockTosRepository, times(0)).setTermsOfServiceAccepted(anyBoolean());
  }

  @Test
  public void testSignInStateChanged_onSignedIn_whenTosNotAccepted() {
    when(mockTosRepository.isTermsOfServiceAccepted()).thenReturn(false);
    when(mockUserRepository.saveUser(any(User.class))).thenReturn(Completable.complete());
    when(mockTosRepository.getTermsOfService()).thenReturn(Maybe.just(TEST_TERMS_OF_SERVICE));

    authenticationManager.signIn();

    assertProgressDialogVisible(false);
    assertNavigate(
        SignInFragmentDirections.showTermsOfService()
            .setTermsOfServiceText(TEST_TERMS_OF_SERVICE.getText()));
    Mockito.verify(mockUserRepository, times(1)).saveUser(TEST_USER);
    Mockito.verify(mockTosRepository, times(0)).setTermsOfServiceAccepted(anyBoolean());
  }

  @Test
  public void testSignInStateChanged_onSignedIn_whenTosNotAcceptedAndFailedToGetRemoteTos() {
    when(mockTosRepository.isTermsOfServiceAccepted()).thenReturn(false);
    when(mockUserRepository.saveUser(any(User.class))).thenReturn(Completable.complete());
    when(mockTosRepository.getTermsOfService()).thenReturn(Maybe.error(new Exception("foo_error")));
    TestObserver<Integer> testErrorObserver = viewModel.getUnrecoverableErrors().test();

    authenticationManager.signIn();

    assertProgressDialogVisible(false);
    Mockito.verify(mockNavigator, times(0)).navigate(any());
    Mockito.verify(mockUserRepository, times(1)).saveUser(TEST_USER);
    testErrorObserver.assertValue(R.string.config_load_error);
    Mockito.verify(mockTosRepository, times(0)).setTermsOfServiceAccepted(anyBoolean());
  }

  @Test
  public void testSignInStateChanged_onSignInError() {
    authenticationManager.error();

    Mockito.verify(mockPopups, times(1)).showError(R.string.sign_in_unsuccessful);
    assertProgressDialogVisible(false);
    assertNavigate(SignInFragmentDirections.showSignInScreen());
    Mockito.verify(mockProjectRepository, times(1)).clearActiveProject();
    Mockito.verify(mockUserRepository, times(1)).clearUserPreferences();
    Mockito.verify(mockTosRepository, times(1)).setTermsOfServiceAccepted(false);
  }

  private static class FakeAuthenticationManager implements AuthenticationManager {

    @Hot(replays = true)
    private final Subject<SignInState> behaviourSubject = BehaviorSubject.create();

    @Override
    public Observable<SignInState> getSignInState() {
      return behaviourSubject;
    }

    @Override
    public User getCurrentUser() {
      return TEST_USER;
    }

    @Override
    public void init() {
      // do nothing
    }

    public void error() {
      behaviourSubject.onNext(new SignInState(new Exception("sign-in error")));
    }

    public void signingIn() {
      behaviourSubject.onNext(new SignInState(State.SIGNING_IN));
    }

    @Override
    public void signIn() {
      behaviourSubject.onNext(new SignInState(TEST_USER));
    }

    @Override
    public void signOut() {
      behaviourSubject.onNext(new SignInState(State.SIGNED_OUT));
    }
  }
}
