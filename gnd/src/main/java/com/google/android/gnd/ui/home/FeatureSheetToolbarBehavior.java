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

package com.google.android.gnd.ui.home;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

/** Defines transitions for the toolbar shown when the feature sheet is displayed. */
public class FeatureSheetToolbarBehavior extends OnBottomSheetChangeBehavior<ViewGroup> {

  public FeatureSheetToolbarBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onBottomSheetChanged(
      CoordinatorLayout parent, ViewGroup toolbarWrapper, HomeScreenMetrics metrics) {
    // Slide toolbar down and fade in, with the toolbar being fully visible as soon the top of the
    // bottom sheet passes the top of the "Add Observation" button.
    float visibilityRatio = metrics.getFeatureBottomSheetVisibilityRatio();
    toolbarWrapper.setAlpha(visibilityRatio);
    toolbarWrapper.setTranslationY(toolbarWrapper.getHeight() * (visibilityRatio - 1.0f));
  }
}
