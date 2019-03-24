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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.home.OnBottomSheetSlideBehavior;

public class MapContainerLayoutBehavior extends OnBottomSheetSlideBehavior<FrameLayout> {
  private static final float SHOW_CROSSHAIRS_THRESHOLD = 0.5f;
  private static final float HIDE_CROSSHAIRS_THRESHOLD = 0.1f;
  private static final float SHOW_BUTTONS_THRESHOLD = 0.1f;
  private static final float HIDE_BUTTONS_THRESOLD = 0.3f;

  public MapContainerLayoutBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onSheetScrolled(
      CoordinatorLayout parent, FrameLayout mapContainerLayout, SheetSlideMetrics metrics) {
    if (metrics.getPeekHeight() <= 0) {
      return;
    }
    View map = mapContainerLayout.findViewById(R.id.map);
    View crosshairs = mapContainerLayout.findViewById(R.id.map_crosshairs);
    View mapButtonLayout = mapContainerLayout.findViewById(R.id.map_btn_layout);
    if (map == null || crosshairs == null || mapButtonLayout == null) {
      // View already destroyed.
      return;
    }
    float visibleToolbarHeight = 0;
    float bottomOffset = Math.min(metrics.getPeekHeight(), metrics.getVisibleHeight());
    float offset = Math.max(bottomOffset - visibleToolbarHeight, 0f);
    float translationY = -offset / 2.0f;
    map.setTranslationY(translationY);
    crosshairs.setTranslationY(translationY);
    metrics.hideWithSheet(crosshairs, SHOW_CROSSHAIRS_THRESHOLD, HIDE_CROSSHAIRS_THRESHOLD);
    metrics.hideWithSheet(mapButtonLayout, SHOW_BUTTONS_THRESHOLD, HIDE_BUTTONS_THRESOLD);
  }
}
