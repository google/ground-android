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
import androidx.navigation.NavDirections;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.repository.TermsOfServiceRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.auth.SignInState;
import com.google.android.gnd.system.auth.SignInState.State;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.signin.SignInFragmentDirections;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
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
  private final UserRepository userRepository;
  private final TermsOfServiceRepository termsOfServiceRepository;
  private final EphemeralPopups popups;

  @Inject
  public MainViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      UserRepository userRepository,
      TermsOfServiceRepository termsOfServiceRepository,
      Navigator navigator,
      AuthenticationManager authenticationManager,
      EphemeralPopups popups,
      Schedulers schedulers) {
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.termsOfServiceRepository = termsOfServiceRepository;
    this.userRepository = userRepository;
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
            .switchMap(this::onSignInStateChange)
            .subscribe(navigator::navigate));
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

  private Observable<NavDirections> onSignInStateChange(SignInState signInState) {
    if (signInState.state() != State.SIGNED_IN) {
      termsOfServiceRepository.setTermsOfServiceAccepted(false);
      featureRepository.setPolygonDialogInfoShown(false);
    }

    switch (signInState.state()) {
      case SIGNED_OUT:
        // TODO: Check auth status whenever fragments resumes.
        return onSignedOut();
      case SIGNING_IN:
        showProgressDialog();
        break;
      case SIGNED_IN:
        return onSignedIn();
      case ERROR:
        Timber.d("Authentication error : %s", signInState.error());
        return onSignInError();
      default:
        Timber.e("Unhandled state: %s", signInState.state());
        break;
    }
    return Observable.never();
  }

  private void showProgressDialog() {
    signInProgressDialogVisibility.postValue(true);
  }

  private void hideProgressDialog() {
    signInProgressDialogVisibility.postValue(false);
  }

  private Observable<NavDirections> onSignInError() {
    popups.showError(R.string.sign_in_unsuccessful);
    return onSignedOut();
  }

  private Observable<NavDirections> onSignedOut() {
    hideProgressDialog();
    projectRepository.clearActiveProject();
    userRepository.clearUserPreferences();
    return Observable.just(SignInFragmentDirections.showSignInScreen());
  }

  private Observable<NavDirections> onSignedIn() {
    hideProgressDialog();
    if (termsOfServiceRepository.isTermsOfServiceAccepted()) {
      return Observable.just(HomeScreenFragmentDirections.showHomeScreen());
    } else {
      return termsOfServiceRepository
          .getTermsOfService()
          .map(TermsOfService::getText)
          .map(text -> SignInFragmentDirections.showTermsOfService().setTermsOfServiceText(text))
          .cast(NavDirections.class)
          .switchIfEmpty(Maybe.just(HomeScreenFragmentDirections.showHomeScreen()))
          .toObservable();
    }
  }

  public LiveData<Boolean> getSignInProgressDialogVisibility() {
    return signInProgressDialogVisibility;
  }
}
