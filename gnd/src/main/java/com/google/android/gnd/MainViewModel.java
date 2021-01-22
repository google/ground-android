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

import static com.google.android.gnd.rx.RxTransformers.switchMapIfPresent;

import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.rx.annotations.Hot;
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

  /** The window insets determined by the activity. */
  @Hot(replays = true)
  private final MutableLiveData<WindowInsetsCompat> windowInsets = new MutableLiveData<>();

  /** The state of sign in progress dialog visibility. */
  @Hot(replays = true)
  private final MutableLiveData<Boolean> signInProgressDialogVisibility = new MutableLiveData<>();

  private final ProjectRepository projectRepository;
  private final FeatureRepository featureRepository;
  private final Navigator navigator;
  private final EphemeralPopups popups;

  @Inject
  public MainViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      UserRepository userRepository,
      Navigator navigator,
      AuthenticationManager authenticationManager,
      EphemeralPopups popups,
      Schedulers schedulers) {
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.navigator = navigator;
    this.popups = popups;

    // TODO: Move to background service.
    disposeOnClear(
        projectRepository
            .getActiveProject()
            .observeOn(schedulers.io())
            .switchMapCompletable(this::syncFeatures)
            .subscribe());

    disposeOnClear(
        authenticationManager
            .getSignInState()
            .compose(switchMapIfPresent(SignInState::getUser, userRepository::saveUser))
            .observeOn(schedulers.ui())
            .subscribe(this::onSignInStateChange));
  }

  /**
   * Keeps local features in sync with remote when a project is active, does nothing when no project
   * is active. The stream never completes; syncing stops when subscriptions are disposed of.
   *
   * @param project the currently active project.
   */
  @Cold(terminates = false)
  private Completable syncFeatures(Optional<Project> project) {
    return project.map(featureRepository::syncFeatures).orElse(Completable.never());
  }

  @Hot(replays = true)
  public LiveData<WindowInsetsCompat> getWindowInsets() {
    return windowInsets;
  }

  void onApplyWindowInsets(WindowInsetsCompat insets) {
    windowInsets.setValue(insets);
  }

  private void onSignInStateChange(SignInState signInState) {
    switch (signInState.state()) {
      case SIGNED_OUT:
        // TODO: Check auth status whenever fragments resumes.
        onSignedOut();
        break;
      case SIGNING_IN:
        showProgressDialog();
        break;
      case SIGNED_IN:
        onSignedIn();
        break;
      case ERROR:
        onSignInError(signInState);
        break;
      default:
        Timber.e("Unhandled state: %s", signInState.state());
        break;
    }
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

  private void onSignedIn() {
    hideProgressDialog();
    navigator.navigate(HomeScreenFragmentDirections.showHomeScreen());
  }

  public LiveData<Boolean> getSignInProgressDialogVisibility() {
    return signInProgressDialogVisibility;
  }
}
