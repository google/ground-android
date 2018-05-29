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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import butterknife.ButterKnife;
import com.google.android.gnd.model.GndDataRepository;
import com.google.android.gnd.rx.RxErrors;
import com.google.android.gnd.service.DataService;
import com.google.android.gnd.system.PermissionsManager;
import com.google.android.gnd.system.PermissionsManager.PermissionsRequest;
import com.google.android.gnd.system.SettingsManager;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequest;
import com.google.android.gnd.ui.common.GndActivity;
import com.google.android.gnd.ui.common.GndViewModelFactory;
import io.reactivex.plugins.RxJavaPlugins;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainActivity extends GndActivity {
  private static final String TAG = MainActivity.class.getSimpleName();

  @Inject
  GndViewModelFactory viewModelFactory;

  @Inject PermissionsManager permissionsManager;

  @Inject SettingsManager settingsManager;

  @Inject DataService dataService;

  @Inject GndDataRepository model;

  private Menu toolbarMenu;
  private MainActivityViewModel viewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Prevent RxJava from force-quitting when multiple Completables terminate with onError.
    RxJavaPlugins.setErrorHandler(t -> RxErrors.logEnhancedStackTrace(t));

    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    initToolbar();
    viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainActivityViewModel.class);

    ViewCompat.setOnApplyWindowInsetsListener(
      getWindow().getDecorView().getRootView(), viewModel::updateWindowInsets);
    permissionsManager.permissionsRequests().subscribe(this::requestPermissions);
    settingsManager.settingsChangeRequests().subscribe(this::requestSettingsChange);
  }

  private void requestPermissions(PermissionsRequest permissionsRequest) {
    ActivityCompat.requestPermissions(
        this, permissionsRequest.getPermissions(), permissionsRequest.getRequestCode());
  }

  private void requestSettingsChange(SettingsChangeRequest settingsChangeRequest) {
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

  private void initToolbar() {
    setSupportActionBar(getToolbar());
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    toolbarMenu = menu;
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.place_sheet_menu, menu);

    return true;
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.toolbar_save_link:
        //        return mainPresenter.onToolbarSaveButtonClick();
    }
    return super.onOptionsItemSelected(item);
  }

  public void hideSoftInput() {
    View view = this.getCurrentFocus();
    if (view != null) {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }

  public Toolbar getToolbar() {
    return findViewById(R.id.toolbar);
  }

  public MenuItem getToolbarSaveButton() {
    return toolbarMenu.findItem(R.id.toolbar_save_link);
  }

  /**
   * The Android permissions API requires this callback to live in an Activity; here we dispatch the
   * result back to the PermissionManager for handling.
   */
  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    settingsManager.onActivityResult(requestCode, resultCode);
  }
}
