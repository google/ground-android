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

package com.google.android.gnd;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import androidx.navigation.NavDirections;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.model.User;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.repository.TermsOfServiceRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.system.auth.FakeAuthenticationManager;
import com.google.android.gnd.system.auth.SignInState;
import com.google.android.gnd.system.auth.SignInState.State;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.signin.SignInFragmentDirections;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@HiltAndroidTest
public class MainViewModelTest extends HiltTestWithRobolectricRunner {

  private static final TermsOfService TEST_TERMS_OF_SERVICE = FakeData.TEST_TERMS_OF_SERVICE;
  private static final Optional<Project> TEST_ACTIVE_PROJECT = Optional.of(FakeData.TEST_PROJECT);
  private static final User TEST_USER = FakeData.TEST_USER;

  @Mock ProjectRepository mockProjectRepository;
  @Mock FeatureRepository mockFeatureRepository;
  @Mock UserRepository mockUserRepository;
  @Mock TermsOfServiceRepository mockTosRepository;
  @Mock EphemeralPopups mockPopups;
  @Mock Navigator mockNavigator;

  @Inject Schedulers schedulers;

  // TODO: Inject this dependency instead of instantiating manually.
  private FakeAuthenticationManager fakeAuthenticationManager;
  private MainViewModel viewModel;

  @Before
  public void setUp() {
    super.setUp();

    // TODO: Add a test for syncFeatures
    when(mockProjectRepository.getActiveProject()).thenReturn(Flowable.just(TEST_ACTIVE_PROJECT));

    fakeAuthenticationManager = new FakeAuthenticationManager();
    viewModel =
        new MainViewModel(
            mockProjectRepository,
            mockFeatureRepository,
            mockUserRepository,
            mockTosRepository,
            mockNavigator,
            fakeAuthenticationManager,
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
    fakeAuthenticationManager.signOut();

    assertProgressDialogVisible(false);
    assertNavigate(SignInFragmentDirections.showSignInScreen());
    Mockito.verify(mockProjectRepository, times(1)).clearActiveProject();
    Mockito.verify(mockUserRepository, times(1)).clearUserPreferences();
    Mockito.verify(mockTosRepository, times(1)).setTermsOfServiceAccepted(false);
  }

  @Test
  public void testSignInStateChanged_onSigningIn() {
    fakeAuthenticationManager.setState(new SignInState(State.SIGNING_IN));

    assertProgressDialogVisible(true);
    Mockito.verify(mockNavigator, times(0)).navigate(any());
    Mockito.verify(mockTosRepository, times(1)).setTermsOfServiceAccepted(false);
  }

  @Test
  public void testSignInStateChanged_onSignedIn_whenTosAccepted() {
    when(mockTosRepository.isTermsOfServiceAccepted()).thenReturn(true);
    when(mockUserRepository.saveUser(any(User.class))).thenReturn(Completable.complete());

    fakeAuthenticationManager.setUser(TEST_USER);
    fakeAuthenticationManager.signIn();

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

    fakeAuthenticationManager.setUser(TEST_USER);
    fakeAuthenticationManager.signIn();

    assertProgressDialogVisible(false);
    assertNavigate(
        SignInFragmentDirections.showTermsOfService()
            .setTermsOfServiceText(TEST_TERMS_OF_SERVICE.getText()));
    Mockito.verify(mockUserRepository, times(1)).saveUser(TEST_USER);
    Mockito.verify(mockTosRepository, times(0)).setTermsOfServiceAccepted(anyBoolean());
  }

  @Test
  public void testSignInStateChanged_onSignInError() {
    fakeAuthenticationManager.setState(new SignInState(new Exception()));

    Mockito.verify(mockPopups, times(1)).showError(R.string.sign_in_unsuccessful);
    assertProgressDialogVisible(false);
    assertNavigate(SignInFragmentDirections.showSignInScreen());
    Mockito.verify(mockProjectRepository, times(1)).clearActiveProject();
    Mockito.verify(mockUserRepository, times(1)).clearUserPreferences();
    Mockito.verify(mockTosRepository, times(1)).setTermsOfServiceAccepted(false);
  }
}
