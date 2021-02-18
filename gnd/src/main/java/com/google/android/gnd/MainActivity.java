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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.gnd.databinding.MainActBinding;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.system.ActivityStreams;
import com.google.android.gnd.system.SettingsManager;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.ProgressDialogs;
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
  @Inject Navigator navigator;
  @Inject UserRepository userRepository;

  private NavHostFragment navHostFragment;
  private MainViewModel viewModel;

  @Nullable private ProgressDialog signInProgressDialog;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    // Make sure this is before calling super.onCreate()
    setTheme(R.style.AppTheme);
    super.onCreate(savedInstanceState);

    // Set up event streams first. Navigator must be listening when auth is first initialized.
    activityStreams
        .getActivityRequests()
        .as(autoDisposable(this))
        .subscribe(callback -> callback.accept(this));
    navigator.getNavigateRequests().as(autoDisposable(this)).subscribe(this::onNavigate);
    navigator.getNavigateUpRequests().as(autoDisposable(this)).subscribe(__ -> navigateUp());

    MainActBinding binding = MainActBinding.inflate(getLayoutInflater());

    setContentView(binding.getRoot());

    navHostFragment =
        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

    viewModel = viewModelFactory.get(this, MainViewModel.class);
    viewModel.getSignInProgressDialogVisibility().observe(this, this::onSignInProgress);
  }

  @Override
  protected void onWindowInsetChanged(WindowInsetsCompat insets) {
    super.onWindowInsetChanged(insets);
    viewModel.onApplyWindowInsets(insets);
  }

  private void onNavigate(NavDirections navDirections) {
    getNavController().navigate(navDirections);
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

  private void onSignInProgress(boolean visible) {
    if (visible) {
      showSignInDialog();
    } else {
      dismissSignInDialog();
    }
  }

  private void showSignInDialog() {
    if (signInProgressDialog == null) {
      signInProgressDialog = ProgressDialogs.modalSpinner(this, R.string.please_wait_logging_in);
    }
    signInProgressDialog.show();
  }

  public void dismissSignInDialog() {
    if (signInProgressDialog != null) {
      signInProgressDialog.dismiss();
      signInProgressDialog = null;
    }
  }
}
