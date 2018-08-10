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
import android.content.IntentSender.SendIntentException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import androidx.navigation.fragment.NavHostFragment;
import butterknife.ButterKnife;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.rx.RxDebug;
import com.google.android.gnd.service.RemoteDataService;
import com.google.android.gnd.system.PermissionsManager;
import com.google.android.gnd.system.PermissionsManager.PermissionsRequest;
import com.google.android.gnd.system.SettingsManager;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequest;
import com.google.android.gnd.ui.common.OnBackListener;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.util.DrawableUtil;
import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.reactivex.plugins.RxJavaPlugins;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainActivity extends AppCompatActivity implements HasSupportFragmentInjector {

  private static final String TAG = MainActivity.class.getSimpleName();

  @Inject ViewModelFactory viewModelFactory;
  @Inject PermissionsManager permissionsManager;
  @Inject SettingsManager settingsManager;
  @Inject RemoteDataService remoteDataService;
  @Inject DataRepository model;
  @Inject DispatchingAndroidInjector<Fragment> fragmentInjector;

  private NavHostFragment navHostFragment;
  private MainViewModel viewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    logLifecycleEvent(this);

    // Prevent RxJava from force-quitting on unhandled errors.
    RxJavaPlugins.setErrorHandler(t -> RxDebug.logEnhancedStackTrace(t));

    super.onCreate(savedInstanceState);

    AndroidInjection.inject(this);

    setContentView(R.layout.main_act);

    ButterKnife.bind(this);

    navHostFragment =
        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

    viewModel = viewModelFactory.get(this, MainViewModel.class);

    ViewCompat.setOnApplyWindowInsetsListener(
        getWindow().getDecorView().getRootView(), viewModel::onApplyWindowInsets);

    permissionsManager
        .getPermissionsRequests()
        .as(autoDisposable(this))
        .subscribe(this::onPermissionsRequest);

    settingsManager
        .getSettingsChangeRequests()
        .as(autoDisposable(this))
        .subscribe(this::onSettingsChangeRequest);
  }

  private void onPermissionsRequest(PermissionsRequest permissionsRequest) {
    Log.d(TAG, "Sending permissions request to system");
    ActivityCompat.requestPermissions(
        this, permissionsRequest.getPermissions(), permissionsRequest.getRequestCode());
  }

  private void onSettingsChangeRequest(SettingsChangeRequest settingsChangeRequest) {
    try {
      // The result of this call is received by {@link #onActivityResult}.
      Log.d(TAG, "Sending settings resolution request");
      settingsChangeRequest
          .getException()
          .startResolutionForResult(this, settingsChangeRequest.getRequestCode());
    } catch (SendIntentException e) {
      // TODO: Report error.
      Log.e(TAG, e.toString());
    }
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
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  /**
   * The Android settings API requires this callback to live in an Activity; here we dispatch the
   * result back to the SettingsManager for handling.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    settingsManager.onActivityResult(requestCode, resultCode);
  }

  @Override
  public final AndroidInjector<Fragment> supportFragmentInjector() {
    return fragmentInjector;
  }

  public void setActionBar(TwoLineToolbar toolbar, int upIconId) {
    setActionBar(toolbar);
    // We override the color here programmatically since calling setHomeAsUpIndicator uses the color
    // of the set icon, not the applied theme. This allows us to change the primary color
    // programmatically without needing to remember to update the icon.
    Drawable icon = new DrawableUtil(getResources()).getDrawable(upIconId, R.color.colorPrimary);
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
    toolbar.setNavigationOnClickListener(__ -> navigateUp());
  }

  private void navigateUp() {
    if (!callFragmentBackHandler()) {
      navHostFragment.getNavController().navigateUp();
    }
  }

  @Override
  public void onBackPressed() {
    if (!callFragmentBackHandler()) {
      super.onBackPressed();
    }
  }

  private boolean callFragmentBackHandler() {
    Fragment currentFragment = getCurrentFragment();
    return currentFragment instanceof OnBackListener && ((OnBackListener) currentFragment).onBack();
  }

  private Fragment getCurrentFragment() {
    return navHostFragment.getChildFragmentManager().findFragmentById(R.id.nav_host_fragment);
  }
}
