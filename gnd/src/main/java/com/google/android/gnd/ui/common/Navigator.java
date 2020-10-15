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

package com.google.android.gnd.ui.common;

import androidx.navigation.NavDirections;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.observationdetails.ObservationDetailsFragment;
import com.google.android.gnd.ui.observationdetails.ObservationDetailsFragmentDirections;
import com.google.android.gnd.ui.offlinebasemap.OfflineBaseMapsFragmentDirections;
import com.google.android.gnd.ui.signin.SignInFragmentDirections;
import com.google.android.gnd.ui.startup.StartupFragmentDirections;
import dagger.hilt.android.scopes.ActivityScoped;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

/**
 * Responsible for abstracting navigation from fragment to fragment. Exposes various actions to
 * ViewModels that cause a NavDirections to be emitted to the observer (in this case, the {@link
 * com.google.android.gnd.MainActivity}, which is expected to pass it to the current {@link
 * androidx.navigation.NavController}.
 */
@ActivityScoped
public class Navigator {
  private final Subject<NavDirections> navigateRequests;
  private final Subject<Object> navigateUpRequests;

  @Inject
  public Navigator() {
    this.navigateRequests = PublishSubject.create();
    this.navigateUpRequests = PublishSubject.create();
  }

  /** Stream of navigation requests for fulfillment by the view layer. */
  public Observable<NavDirections> getNavigateRequests() {
    return navigateRequests;
  }

  public Observable<Object> getNavigateUpRequests() {
    return navigateUpRequests;
  }

  /** Navigates up one level on the back stack. */
  public void navigateUp() {
    navigateUpRequests.onNext(new Object());
  }

  private void navigate(NavDirections n) {
    navigateRequests.onNext(n);
  }

  /**
   * Navigates from a {@link com.google.android.gnd.ui.home.HomeScreenFragment} to a {@link
   * ObservationDetailsFragment} populated with the specified observation.
   */
  public void showObservationDetails(String projectId, String featureId, String observationId) {
    navigate(
        HomeScreenFragmentDirections.showObservationDetails(projectId, featureId, observationId));
  }

  /**
   * Navigates from a {@link com.google.android.gnd.ui.home.HomeScreenFragment} to a {@link
   * com.google.android.gnd.ui.offlinebasemap.selector.OfflineBaseMapSelectorFragment}.
   */
  public void showOfflineAreaSelector() {
    navigate(OfflineBaseMapsFragmentDirections.showOfflineAreaSelector());
  }

  /**
   * Navigates from the {@link com.google.android.gnd.ui.home.HomeScreenFragment} to a {@link
   * EditObservationFragment} initialized with a new empty observation using the specified form.
   */
  public void addObservation(String projectId, String featureId, String formId) {
    navigate(HomeScreenFragmentDirections.addObservation(projectId, featureId, formId));
  }

  /**
   * Navigates from the {@link ObservationDetailsFragment} to a {@link EditObservationFragment}
   * populated with the specified observation.
   */
  public void editObservation(String projectId, String featureId, String observationId) {
    navigate(
        ObservationDetailsFragmentDirections.editObservation(projectId, featureId, observationId));
  }

  /** Navigates to the home screen. */
  public void showHomeScreen(int currentNavDestinationId) {
    switch (currentNavDestinationId) {
      case R.id.startup_fragment:
        navigate(StartupFragmentDirections.proceedDirectlyToHomeScreen());
        break;
      case R.id.sign_in_fragment:
        navigate(SignInFragmentDirections.proceedToHomeScreen());
        break;
      default:
        throw new IllegalArgumentException(currentNavDestinationId + " id not found");
    }
  }

  /** Navigates to the sign in screen. */
  public void showSignInScreen(int currentNavDestinationId) {
    switch (currentNavDestinationId) {
      case R.id.startup_fragment:
        navigate(StartupFragmentDirections.proceedToSignInScreen());
        break;
      case R.id.home_screen_fragment:
        navigate(HomeScreenFragmentDirections.fromHomeScreenToSignInScreen());
        break;
      case R.id.sign_in_fragment:
        // Sign in screen already active.
        break;
      default:
        throw new IllegalArgumentException(currentNavDestinationId + " id not found");
    }
  }

  public void showOfflineAreas() {
    navigate(HomeScreenFragmentDirections.showOfflineAreas());
  }

  public void showSettings() {
    navigate(HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity());
  }

  public void showOfflineAreaViewer(String offlineAreaId) {
    navigate(OfflineBaseMapsFragmentDirections.viewOfflineArea(offlineAreaId));
  }
}
