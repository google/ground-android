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

package com.google.android.gnd.ui.home;

import static com.google.android.gnd.ui.home.OnBottomSheetSlideBehavior.SheetSlideMetrics.scale;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.gnd.R;

public class BottomSheetChromeBehavior extends OnBottomSheetSlideBehavior<ViewGroup> {
  // TODO: Refactor transitions into "TransitionEffect" classes.
  private static final float HIDE_SCRIM_THRESHOLD = 0.0f;
  private static final float SHOW_SCRIM_THRESHOLD = 0.1f;
  private static final float HIDE_ADD_BUTTON_THRESHOLD = 0.3f;
  private static final float SHOW_ADD_BUTTON_THRESHOLD = 0.5f;

  public BottomSheetChromeBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onSheetScrolled(
      CoordinatorLayout parent, ViewGroup layout, SheetSlideMetrics metrics) {
    View mapScrim = parent.findViewById(R.id.map_scrim);
    View hamburgerButton = parent.findViewById(R.id.hamburger_btn);
    View bottomSheetScrim = layout.findViewById(R.id.bottom_sheet_bottom_inset_scrim);
    View addObservationButton = layout.findViewById(R.id.add_observation_btn);
    ViewGroup toolbarWrapper = layout.findViewById(R.id.toolbar_wrapper);
    metrics.showWithSheet(mapScrim, 0.75f, 1.0f);
    metrics.showWithSheet(bottomSheetScrim, HIDE_SCRIM_THRESHOLD, SHOW_SCRIM_THRESHOLD);
    metrics.hideWithSheet(hamburgerButton, HIDE_ADD_BUTTON_THRESHOLD, SHOW_ADD_BUTTON_THRESHOLD);
    metrics.showWithSheet(
        addObservationButton, HIDE_ADD_BUTTON_THRESHOLD, SHOW_ADD_BUTTON_THRESHOLD);
    toolbarWrapper.setBackgroundColor(layout.getResources().getColor(R.color.colorPrimary));
    toolbarWrapper.setTranslationY(
        scale(metrics.getVisibleRatio(), 0.3f, 0.5f, -toolbarWrapper.getHeight(), 0));
    metrics.showWithSheet(toolbarWrapper.getBackground(), 0.9f, 1);
  }
}
