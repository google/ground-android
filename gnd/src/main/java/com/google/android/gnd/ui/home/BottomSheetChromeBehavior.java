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

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

/** Defines transitions for the bottom sheet chrom shown when the feature sheet is displayed. */
public class BottomSheetChromeBehavior extends OnBottomSheetChangeBehavior<ViewGroup> {

  public BottomSheetChromeBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onBottomSheetChanged(
      CoordinatorLayout parent, ViewGroup layout, HomeScreenMetrics metrics) {
    // Fade in the bottom scrim and "Add Observation" button, with both being fully visible as soon
    // as the top of the bottom sheet passes the top of the "Add Observation" button.

    // Chrome is fully opaque as top of bottom sheet passes Add Observation button.
    layout.setAlpha(metrics.getBottomSheetVisibilityRatio());
  }
}
