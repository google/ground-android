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

import static android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED;
import static android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gnd.model.GndDataRepository;
import com.google.android.gnd.service.DataService;
import com.google.android.gnd.system.PermissionsManager;
import com.google.android.gnd.system.PermissionsManager.PermissionsRequest;
import com.google.android.gnd.system.SettingsManager;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequest;
import com.google.android.gnd.ui.common.GndActivity;
import com.google.android.gnd.ui.sheet.DataSheetScrollView;
import com.google.android.gnd.ui.util.ViewUtil;
import io.reactivex.plugins.RxJavaPlugins;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainActivity extends GndActivity {
  private static final String TAG = MainActivity.class.getSimpleName();

  private MainPresenter mainPresenter;

  @BindView(R.id.add_place_btn)
  FloatingActionButton addPlaceBtn;

  private ProgressDialog progressDialog;
  private Menu toolbarMenu;

  @Inject
  PermissionsManager permissionsManager;

  @Inject
  SettingsManager settingsManager;

  @Inject
  DataService dataService;

  @Inject
  GndDataRepository model;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Prevent RxJava from force-quitting when multiple Completables terminate with onError.
    // TODO: Investigate why this is happening and fix root cause, log instead of eating errors.
    RxJavaPlugins.setErrorHandler(t -> { });

    super.onCreate(savedInstanceState);

    this.mainPresenter = new MainPresenter(this, model);

    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    initToolbar();
    updatePaddingForWindowInsets();
    mainPresenter.onCreate(savedInstanceState);
    View decorView = getWindow().getDecorView();
    permissionsManager.permissionsRequests().subscribe(this::requestPermissions);
    settingsManager.settingsChangeRequests().subscribe(this::requestSettingsChange);
    // Sheet doesn't scroll properly w/translucent status due to obscure Android bug. This should
    // be resolved once add/edit is in its own fragment that uses fitsSystemWindows. For now we
    // just expand the sheet when focus + layout change (i.e., keyboard appeared).
    decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
      View newFocus = getCurrentFocus();
      if (newFocus != null) {
        DataSheetScrollView dataSheetView = getDataSheetView();
        BottomSheetBehavior behavior = (BottomSheetBehavior) ((CoordinatorLayout.LayoutParams) dataSheetView
          .getLayoutParams())
          .getBehavior();
        if (behavior.getState() == STATE_COLLAPSED) {
          behavior.setState(STATE_EXPANDED);
        }
      }
    });
  }

  private void requestPermissions(PermissionsRequest permissionsRequest) {
    ActivityCompat.requestPermissions(
      this, permissionsRequest.getPermissions(), permissionsRequest.getRequestCode());
  }


  private void requestSettingsChange(SettingsChangeRequest settingsChangeRequest) {
    try {
      // The result of this call is received by {@link #onActivityResult}.
      Log.d(TAG, "Sending settings resolution request");
      settingsChangeRequest.getException()
                           .startResolutionForResult(this, settingsChangeRequest.getRequestCode());
    } catch (SendIntentException e) {
      // TODO: Report error.
      Log.e(TAG, e.toString());
    }
  }

  private void updatePaddingForWindowInsets() {
    FrameLayout toolbarWrapper = findViewById(R.id.toolbar_wrapper);
    // TODO: Each view should consume its own insets and update the insets for consumption by
    // child views.
    ViewCompat.setOnApplyWindowInsetsListener(
      toolbarWrapper,
      (v, insets) -> {
        int bottomPadding = insets.getSystemWindowInsetBottom();
        int topPadding = insets.getSystemWindowInsetTop();
        View dataSheetWrapper = findViewById(R.id.place_details_fragment);
        View dataSheetLayout = findViewById(R.id.data_sheet_layout);
        View bottomSheetScrim = findViewById(R.id.bottom_sheet_scrim);
        View mapBtnLayout = findViewById(R.id.map_btn_layout);
        View recordBtnLayout = findViewById(R.id.record_btn_layout);
        dataSheetLayout.setMinimumHeight(
          ViewUtil.getScreenHeight(MainActivity.this) - topPadding);
        dataSheetWrapper.setPadding(0, topPadding, 0, bottomPadding);
        toolbarWrapper.setPadding(0, topPadding, 0, 0);
        bottomSheetScrim.setMinimumHeight(bottomPadding);
        mapBtnLayout.setTranslationY(-bottomPadding);
        recordBtnLayout.setTranslationY(-bottomPadding);
        return insets.replaceSystemWindowInsets(0, 0, 0, insets.getSystemWindowInsetBottom());
      });
  }

  public void showProjectLoadingDialog() {
    progressDialog = new ProgressDialog(this);
    progressDialog.setMessage(getResources().getString(R.string.project_loading_please_wait));
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setCancelable(false);
    progressDialog.setCanceledOnTouchOutside(false);
    progressDialog.show();
  }

  public void dismissLoadingDialog() {
    progressDialog.dismiss();
  }

  public void enableAddPlaceButton() {
    addPlaceBtn.setBackgroundTintList(
      ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
  }

  public void showErrorMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private void initToolbar() {
    setSupportActionBar(getToolbar());
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    toolbarMenu = menu;
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.place_header_menu, menu);

    return true;
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.toolbar_save_link:
        return mainPresenter.onToolbarSaveButtonClick();
    }
    return super.onOptionsItemSelected(item);
  }

  public FloatingActionButton getAddPlaceButton() {
    return addPlaceBtn;
  }

  public FloatingActionButton getAddRecordButton() {
    return (FloatingActionButton) findViewById(R.id.add_record_btn);
  }

  public DataSheetScrollView getDataSheetView() {
    return findViewById(R.id.data_sheet);
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
