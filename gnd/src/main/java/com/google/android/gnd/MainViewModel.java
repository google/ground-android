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
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.auth.SignInState;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.signin.SignInFragmentDirections;
import io.reactivex.Completable;
import javax.inject.Inject;
import timber.log.Timber;

/** Top-level view model representing state of the {@link MainActivity} shared by all fragments. */
@SharedViewModel
public class MainViewModel extends AbstractViewModel {

  private final ProjectRepository projectRepository;
  private final FeatureRepository featureRepository;
  private final UserRepository userRepository;
  private final Navigator navigator;
  private final Application context;
  private MutableLiveData<WindowInsetsCompat> windowInsetsLiveData;

  @Inject
  public MainViewModel(
      AuthenticationManager authenticationManager,
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      UserRepository userRepository,
      Navigator navigator,
      Application context) {
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.userRepository = userRepository;
    this.navigator = navigator;
    this.context = context;
    windowInsetsLiveData = new MutableLiveData<>();

    // TODO: Move to background service.
    disposeOnClear(
        projectRepository
            .getActiveProjectOnceAndStream()
            .switchMapCompletable(this::syncFeatures)
            .subscribe());

    // TODO: Remove once we switch to persisted auth tokens / multiple offline users.
    disposeOnClear(
        authenticationManager
            .getSignInState()
            .switchMapCompletable(this::onSignInStateChange)
            .subscribe());
  }

  /**
   * Keeps local features in sync with remote when a project is active, does nothing when no project
   * is active. The stream never completes; syncing stops when subscriptions are disposed of.
   *
   * @param projectLoadable the load state of the currently active project.
   */
  private Completable syncFeatures(Loadable<Project> projectLoadable) {
    return projectLoadable.value().map(featureRepository::syncFeatures).orElse(Completable.never());
  }

  public LiveData<WindowInsetsCompat> getWindowInsets() {
    return windowInsetsLiveData;
  }

  void onApplyWindowInsets(WindowInsetsCompat insets) {
    windowInsetsLiveData.setValue(insets);
  }

  private Completable onSignInStateChange(SignInState signInState) {
    Timber.d("Auth status change: %s", signInState.state());
    switch (signInState.state()) {
      case SIGNING_IN:
        // No-op: Sign in modal shown.
        break;
      case SIGNED_IN:
        return onSignedIn(signInState.getUser().get());
      case SIGNED_OUT:
        onSignedOut();
        break;
      case ERROR:
        // TODO: Multiple representations of the same state (e.g., ERROR + error()) lead to more
        // boilerplate needed to prevent runtime errors. We should simplify this.
        onAuthenticationError(signInState.error().orElseGet(IllegalStateException::new));
        break;
      default:
        onAuthenticationError(
            new UnsupportedOperationException("Unknown sign in state: " + signInState.state()));
    }
    return Completable.complete();
  }

  private Completable onSignedIn(User user) {
    return userRepository
        .saveUser(user)
        .andThen(
            Completable.fromRunnable(
                () -> navigator.navigate(HomeScreenFragmentDirections.showHomeScreen())));
  }

  private void onSignedOut() {
    projectRepository.clearActiveProject();
    navigator.navigate(SignInFragmentDirections.showSignInScreen());
  }

  private void onAuthenticationError(Throwable err) {
    Timber.d(err, "Authentication error");
    EphemeralPopups.showError(context, R.string.sign_in_unsuccessful);
    onSignedOut();
  }
}
