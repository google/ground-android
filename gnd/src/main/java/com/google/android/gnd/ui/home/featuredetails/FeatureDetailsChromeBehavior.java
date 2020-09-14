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

package com.google.android.gnd.ui.home.featuredetails;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.home.BottomSheetDependentBehavior;
import com.google.android.gnd.ui.home.BottomSheetMetrics;
import timber.log.Timber;

/**
 * Defines behavior of the feature details UI elements (bottom sheet and chrome) when the bottom
 * sheet is scrolled, collapsed, or expanded.
 */
public class FeatureDetailsChromeBehavior extends BottomSheetDependentBehavior<ViewGroup> {
  public FeatureDetailsChromeBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onBottomSheetChanged(
      CoordinatorLayout parent, ViewGroup layout, BottomSheetMetrics metrics) {
    Timber.d("onBottomSheetChanged");
    ViewGroup toolbarWrapper = layout.findViewById(R.id.toolbar_wrapper);
    View bottomSheetScrim = layout.findViewById(R.id.bottom_sheet_bottom_inset_scrim);
    View hamburgerButton = parent.findViewById(R.id.hamburger_btn);
    View mapScrim = parent.findViewById(R.id.map_scrim);

    // Fade in the bottom scrim and "Add Observation" button, with both being fully visible as soon
    // as the top of the bottom sheet passes the top of the "Add Observation" button.
    float revealRatio = metrics.getRevealRatio();
    float hideRatio = 1.0f - revealRatio;
    layout.setAlpha(revealRatio);
    mapScrim.setAlpha(metrics.getExpansionRatio());
    bottomSheetScrim.setAlpha(revealRatio);
    toolbarWrapper.setAlpha(revealRatio);
    toolbarWrapper.setTranslationY(-toolbarWrapper.getHeight() * hideRatio);
    hamburgerButton.setAlpha(hideRatio);
  }
}
