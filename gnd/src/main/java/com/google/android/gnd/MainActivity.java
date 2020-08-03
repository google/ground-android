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

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.gnd.databinding.MainActBinding;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.system.ActivityStreams;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.SettingsManager;
import com.google.android.gnd.system.auth.SignInState;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.ViewModelFactory;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * The app's main activity. The app consists of multiples Fragments that live under this activity.
 */
@AndroidEntryPoint
public class MainActivity extends AbstractActivity {

  @Inject ActivityStreams activityStreams;
  @Inject ViewModelFactory viewModelFactory;
  @Inject SettingsManager settingsManager;
  @Inject AuthenticationManager authenticationManager;
  @Inject Navigator navigator;
  @Inject UserRepository userRepository;
  private NavHostFragment navHostFragment;
  private MainViewModel viewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Make sure this is before calling super.onCreate()
    setTheme(R.style.AppTheme);
    super.onCreate(savedInstanceState);

    MainActBinding binding = MainActBinding.inflate(getLayoutInflater());

    setContentView(binding.getRoot());

    navHostFragment =
        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

    viewModel = viewModelFactory.get(this, MainViewModel.class);

    activityStreams
        .getActivityRequests()
        .as(autoDisposable(this))
        .subscribe(callback -> callback.accept(this));

    // TODO: Remove once we switch to persisted auth tokens / multiple offline users.
    authenticationManager
        .getSignInState()
        .as(autoDisposable(this))
        .subscribe(this::onSignInStateChange);

    navigator.getNavigateRequests().as(autoDisposable(this)).subscribe(this::onNavigate);
    navigator.getNavigateUpRequests().as(autoDisposable(this)).subscribe(__ -> navigateUp());
  }

  @Override
  protected void onWindowInsetChanged(WindowInsetsCompat insets) {
    super.onWindowInsetChanged(insets);
    viewModel.onApplyWindowInsets(insets);
  }

  private void onNavigate(NavDirections navDirections) {
    getNavController().navigate(navDirections);
  }

  private void onSignInStateChange(SignInState signInState) {
    Timber.d("Auth status change: %s", signInState.state());
    switch (signInState.state()) {
      case SIGNED_OUT:
        // TODO: Check auth status whenever fragments resumes.
        viewModel.onSignedOut(getCurrentNavDestinationId());
        break;
      case SIGNING_IN:
        // TODO: Show/hide spinner.
        break;
      case SIGNED_IN:
        userRepository
            .saveUser(signInState.getUser().get())
            .as(autoDisposable(this))
            .subscribe(() -> viewModel.onSignedIn(getCurrentNavDestinationId()));
        break;
      case ERROR:
        onSignInError(signInState);
        break;
      default:
        Timber.e("Unhandled state: %s", signInState.state());
        break;
    }
  }

  private void onSignInError(SignInState signInState) {
    Timber.d("Authentication error : %s", signInState.error());
    EphemeralPopups.showError(this, R.string.sign_in_unsuccessful);
    viewModel.onSignedOut(getCurrentNavDestinationId());
  }

  /**
   * The Android permissions API requires this callback to live in an Activity; here we dispatch the
   * result back to the PermissionManager for handling.
   */
  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Timber.d("Permission result received");
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    activityStreams.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  /**
   * The Android settings API requires this callback to live in an Activity; here we dispatch the
   * result back to the SettingsManager for handling.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    Timber.d("Activity result received");
    super.onActivityResult(requestCode, resultCode, intent);
    activityStreams.onActivityResult(requestCode, resultCode, intent);
  }

  /** Override up button behavior to use Navigation Components back stack. */
  @Override
  public boolean onSupportNavigateUp() {
    return navigateUp();
  }

  private boolean navigateUp() {
    return getNavController().navigateUp();
  }

  private NavController getNavController() {
    return navHostFragment.getNavController();
  }

  private int getCurrentNavDestinationId() {
    NavDestination destination = getNavController().getCurrentDestination();
    if (destination == null) {
      return -1;
    }
    return destination.getId();
  }

  @Override
  protected void onToolbarUpClicked() {
    if (!dispatchBackPressed()) {
      navigateUp();
    }
  }

  @Override
  public void onBackPressed() {
    if (!dispatchBackPressed()) {
      super.onBackPressed();
    }
  }

  private boolean dispatchBackPressed() {
    Fragment currentFragment = getCurrentFragment();
    return currentFragment instanceof BackPressListener
        && ((BackPressListener) currentFragment).onBack();
  }

  private Fragment getCurrentFragment() {
    return navHostFragment.getChildFragmentManager().findFragmentById(R.id.nav_host_fragment);
  }
}
