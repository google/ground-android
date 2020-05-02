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

package com.google.android.gnd.ui.home.mapcontainer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.home.HomeScreenMetrics;
import com.google.android.gnd.ui.home.OnBottomSheetChangeBehavior;

/**
 * Defines transitions for the map, map controls, and cross hairs when the feature sheet is
 * scrolled.
 */
public class MapContainerLayoutBehavior extends OnBottomSheetChangeBehavior<FrameLayout> {
  public MapContainerLayoutBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onBottomSheetChanged(
      CoordinatorLayout parent, FrameLayout mapContainerLayout, HomeScreenMetrics metrics) {
    if (metrics.getBottomSheetPeekHeight() <= 0) {
      // Behavior not yet initialized.
      return;
    }
    View map = mapContainerLayout.findViewById(R.id.map);
    View crossHairs = mapContainerLayout.findViewById(R.id.map_crosshairs);
    View mapButtonLayout = mapContainerLayout.findViewById(R.id.map_btn_layout);
    if (map == null || crossHairs == null || mapButtonLayout == null) {
      // View already destroyed.
      return;
    }

    // Fade out cross hairs and map buttons as bottom sheet appears.
    float alpha = 1.0f - metrics.getBottomSheetVisibilityRatio();
    crossHairs.setAlpha(alpha);
    mapButtonLayout.setAlpha(alpha);

    // Slide map and cross hairs up as sheet is scrolled below peek height.
    float offset =
        Math.min(metrics.getBottomSheetPeekHeight(), metrics.getBottomSheetVisibleHeight());
    float translationY = -offset / 2.0f;
    map.setTranslationY(translationY);
    crossHairs.setTranslationY(translationY);
  }
}
