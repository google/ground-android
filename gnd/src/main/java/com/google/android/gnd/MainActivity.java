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
import static com.google.android.gnd.util.Debug.logLifecycleEvent;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import butterknife.ButterKnife;
import com.google.android.gnd.rx.RxDebug;
import com.google.android.gnd.system.ActivityStreams;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.system.AuthenticationManager.SignInState;
import com.google.android.gnd.system.SettingsManager;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.util.DrawableUtil;
import dagger.android.support.DaggerAppCompatActivity;
import io.reactivex.plugins.RxJavaPlugins;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The app's main and only activity. The app consists of multiples Fragments that live under this
 * activity.
 */
@Singleton
public class MainActivity extends DaggerAppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  @Inject ActivityStreams activityStreams;
  @Inject ViewModelFactory viewModelFactory;
  @Inject SettingsManager settingsManager;
  @Inject AuthenticationManager authenticationManager;
  @Inject Navigator navigator;
  private NavHostFragment navHostFragment;
  private MainViewModel viewModel;
  private DrawableUtil drawableUtil;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    logLifecycleEvent(this);
    drawableUtil = new DrawableUtil(getResources());

    // Prevent RxJava from force-quitting on unhandled errors.
    RxJavaPlugins.setErrorHandler(t -> RxDebug.logEnhancedStackTrace(t));

    // Make sure this is before calling super.onCreate()
    setTheme(R.style.AppTheme);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main_act);

    ButterKnife.bind(this);

    navHostFragment =
        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

    viewModel = viewModelFactory.get(this, MainViewModel.class);

    ViewCompat.setOnApplyWindowInsetsListener(
        getWindow().getDecorView().getRootView(), viewModel::onApplyWindowInsets);

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

  private void onNavigate(NavDirections navDirections) {
    getNavController().navigate(navDirections);
  }

  private void onSignInStateChange(SignInState signInState) {
    Log.d(TAG, "Auth status change: " + signInState.state());
    switch (signInState.state()) {
      case SIGNED_OUT:
        // TODO: Check auth status whenever fragments resumes.
        viewModel.onSignedOut(getCurrentNavDestinationId());
        break;
      case SIGNING_IN:
        // TODO: Show/hide spinner.
        break;
      case SIGNED_IN:
        // TODO: Store/update user profile and image locally.
        viewModel.onSignedIn(getCurrentNavDestinationId());
        break;
      case ERROR:
        onSignInError(signInState);
        break;
    }
  }

  private void onSignInError(SignInState signInState) {
    Log.d(TAG, "Authentication error", signInState.error().orElse(null));
    EphemeralPopups.showError(this, R.string.sign_in_unsuccessful);
    viewModel.onSignedOut(getCurrentNavDestinationId());
  }

  @Override
  protected void onStart() {
    logLifecycleEvent(this);
    super.onStart();
  }

  @Override
  protected void onResume() {
    logLifecycleEvent(this);
    super.onResume();
  }

  @Override
  protected void onPause() {
    logLifecycleEvent(this);
    super.onPause();
  }

  @Override
  protected void onStop() {
    logLifecycleEvent(this);
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    logLifecycleEvent(this);
    super.onDestroy();
  }

  /**
   * The Android permissions API requires this callback to live in an Activity; here we dispatch the
   * result back to the PermissionManager for handling.
   */
  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Log.d(TAG, "Permission result received");
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    activityStreams.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  /**
   * The Android settings API requires this callback to live in an Activity; here we dispatch the
   * result back to the SettingsManager for handling.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    Log.d(TAG, "Activity result received");
    super.onActivityResult(requestCode, resultCode, intent);
    activityStreams.onActivityResult(requestCode, resultCode, intent);
  }

  public void setActionBar(TwoLineToolbar toolbar, int upIconId) {
    setActionBar(toolbar);
    // We override the color here programmatically since calling setHomeAsUpIndicator uses the color
    // of the set icon, not the applied theme. This allows us to change the primary color
    // programmatically without needing to remember to update the icon.
    Drawable icon = drawableUtil.getDrawable(upIconId, R.color.colorAccent);
    getSupportActionBar().setHomeAsUpIndicator(icon);
  }

  public void setActionBar(TwoLineToolbar toolbar) {
    setSupportActionBar(toolbar);

    // Workaround to get rid of application title from toolbar. Simply setting "" here or in layout
    // XML doesn't work.
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    // TODO: Remove this workaround once setupActionBarWithNavController() works with custom
    // Toolbars (https://issuetracker.google.com/issues/109868820).
    toolbar.setNavigationOnClickListener(__ -> onToolbarUpClicked());
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
    if (destination != null) {
      return destination.getId();
    }
    return -1;
  }

  private void onToolbarUpClicked() {
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
