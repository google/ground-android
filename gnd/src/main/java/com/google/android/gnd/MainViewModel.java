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

import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.annotations.States;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.auth.SignInState;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.signin.SignInFragmentDirections;
import io.reactivex.Completable;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

/** Top-level view model representing state of the {@link MainActivity} shared by all fragments. */
@SharedViewModel
public class MainViewModel extends AbstractViewModel {

  @States private final MutableLiveData<WindowInsetsCompat> windowInsets = new MutableLiveData<>();

  @States
  private final MutableLiveData<Boolean> signInProgressDialogVisibility = new MutableLiveData<>();

  private final ProjectRepository projectRepository;
  private final FeatureRepository featureRepository;
  private final UserRepository userRepository;
  private final Navigator navigator;
  private final EphemeralPopups popups;

  @Inject
  public MainViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      UserRepository userRepository,
      Navigator navigator,
      AuthenticationManager authenticationManager,
      EphemeralPopups popups) {
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.userRepository = userRepository;
    this.navigator = navigator;
    this.popups = popups;

    // TODO: Move to background service.
    disposeOnClear(
        projectRepository.getActiveProject().switchMapCompletable(this::syncFeatures).subscribe());

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
   * @param project the currently active project.
   */
  private Completable syncFeatures(Optional<Project> project) {
    return project.map(featureRepository::syncFeatures).orElse(Completable.never());
  }

  public LiveData<WindowInsetsCompat> getWindowInsets() {
    return windowInsets;
  }

  void onApplyWindowInsets(WindowInsetsCompat insets) {
    windowInsets.setValue(insets);
  }

  private Completable onSignInStateChange(SignInState signInState) {
    Timber.d("Auth status change: %s", signInState.state());
    switch (signInState.state()) {
      case SIGNED_OUT:
        // TODO: Check auth status whenever fragments resumes.
        onSignedOut();
        break;
      case SIGNING_IN:
        showProgressDialog();
        break;
      case SIGNED_IN:
        User user = signInState.getUser().orElseThrow(IllegalStateException::new);
        return userRepository.saveUser(user).doOnComplete(this::onSignedIn);
      case ERROR:
        onSignInError(signInState);
        break;
      default:
        Timber.e("Unhandled state: %s", signInState.state());
        break;
    }
    return Completable.complete();
  }

  private void showProgressDialog() {
    signInProgressDialogVisibility.postValue(true);
  }

  private void hideProgressDialog() {
    signInProgressDialogVisibility.postValue(false);
  }

  private void onSignInError(SignInState signInState) {
    Timber.d("Authentication error : %s", signInState.error());
    popups.showError(R.string.sign_in_unsuccessful);
    onSignedOut();
  }

  void onSignedOut() {
    hideProgressDialog();
    projectRepository.clearActiveProject();
    navigator.navigate(SignInFragmentDirections.showSignInScreen());
  }

  void onSignedIn() {
    hideProgressDialog();
    navigator.navigate(HomeScreenFragmentDirections.showHomeScreen());
  }

  public LiveData<Boolean> getSignInProgressDialogVisibility() {
    return signInProgressDialogVisibility;
  }
}
