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
import com.google.android.gnd.NavGraphDirections;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.recorddetails.RecordDetailsFragmentDirections;
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
   * com.google.android.gnd.ui.recorddetails.RecordDetailsFragment} populated with the specified
   * record.
   */
  public void showRecordDetails(String projectId, String featureId, String recordId) {
    navigate(HomeScreenFragmentDirections.showRecordDetails(projectId, featureId, recordId));
  }

  /**
   * Navigates from a {@link com.google.android.gnd.ui.home.HomeScreenFragment} to a {@link
   * com.google.android.gnd.ui.basemapselector.BasemapSelectorFragment}.
   */
  public void showBasemapSelector() {
    navigate(HomeScreenFragmentDirections.showBasemapSelector());
  }

  /**
   * Navigates from the {@link com.google.android.gnd.ui.home.HomeScreenFragment} to a {@link
   * com.google.android.gnd.ui.editrecord.EditRecordFragment} initialized with a new empty record
   * using the specified form.
   */
  public void addRecord(String projectId, String featureId, String formId) {
    navigate(HomeScreenFragmentDirections.addRecord(projectId, featureId, formId));
  }

  /**
   * Navigates from the {@link com.google.android.gnd.ui.recorddetails.RecordDetailsFragment} to a
   * {@link com.google.android.gnd.ui.editrecord.EditRecordFragment} populated with the specified
   * record.
   */
  public void editRecord(String projectId, String featureId, String recordId) {
    navigate(RecordDetailsFragmentDirections.editRecord(projectId, featureId, recordId));
  }

  /** Navigates to the home screen. */
  public void showHomeScreen() {
    navigate(NavGraphDirections.showHomeScreen());
  }

  /** Navigates to the sign in screen. */
  public void showSignInScreen() {
    navigate(NavGraphDirections.showSignInScreen());
  }

  public void showOfflineAreaManager() {
    navigate(HomeScreenFragmentDirections.showOfflineAreaManager());
  }
}
