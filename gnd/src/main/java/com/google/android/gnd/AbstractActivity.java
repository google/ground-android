/*
 * Copyright 2020 Google LLC
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

import static com.google.android.gnd.util.Debug.logLifecycleEvent;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.util.DrawableUtil;
import dagger.android.support.DaggerAppCompatActivity;

/** Base activity class containing common helper methods. */
public abstract class AbstractActivity extends DaggerAppCompatActivity {

  private DrawableUtil drawableUtil;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    logLifecycleEvent(this);
    super.onCreate(savedInstanceState);
    drawableUtil = new DrawableUtil(getResources());

    ViewCompat.setOnApplyWindowInsetsListener(
        getWindow().getDecorView().getRootView(),
        (view, insetsCompat) -> {
          onWindowInsetChanged(insetsCompat);
          return insetsCompat;
        });
  }

  /** Adjust UI elements with respect to top/bottom insets. */
  protected void onWindowInsetChanged(WindowInsetsCompat insetsCompat) {}

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

  public void setActionBar(TwoLineToolbar toolbar, int upIconId) {
    setActionBar(toolbar, false);
    // We override the color here programmatically since calling setHomeAsUpIndicator uses the color
    // of the set icon, not the applied theme. This allows us to change the primary color
    // programmatically without needing to remember to update the icon.
    Drawable icon = drawableUtil.getDrawable(upIconId, R.color.colorAccent);
    getSupportActionBar().setHomeAsUpIndicator(icon);
  }

  public void setActionBar(TwoLineToolbar toolbar, boolean showTitle) {
    setSupportActionBar(toolbar);

    // Workaround to get rid of application title from toolbar. Simply setting "" here or in layout
    // XML doesn't work.
    getSupportActionBar().setDisplayShowTitleEnabled(showTitle);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    // TODO: Remove this workaround once setupActionBarWithNavController() works with custom
    // Toolbars (https://issuetracker.google.com/issues/109868820).
    toolbar.setNavigationOnClickListener(__ -> onToolbarUpClicked());
  }

  protected void onToolbarUpClicked() {
    finish();
  }
}
