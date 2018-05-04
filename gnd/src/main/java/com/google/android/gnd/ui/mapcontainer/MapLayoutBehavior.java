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

package com.google.android.gnd.ui.mapcontainer;

import android.content.Context;
import android.os.Parcelable;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.OnSheetSlideBehavior;

public class MapLayoutBehavior extends OnSheetSlideBehavior<RelativeLayout> {
  private static final float SHOW_CROSSHAIRS_THRESHOLD = 0.5f;
  private static final float HIDE_CROSSHAIRS_THRESHOLD = 0.1f;
  private final MainActivity activity;
  private View toolbarWrapper;
  private ImageView crosshairs;
  private View map;

  public MapLayoutBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
    activity = (MainActivity) context;
  }

  @Override
  public void onRestoreInstanceState(
    CoordinatorLayout parent, RelativeLayout child, Parcelable state) {
    super.onRestoreInstanceState(parent, child, state);
  }

  @Override
  protected void onSheetScrolled(
    CoordinatorLayout parent, RelativeLayout layout, SheetSlideMetrics metrics) {
    if (metrics.getPeekHeight() <= 0) {
      return;
    }
    slideMap(layout, metrics);
    fadeCrosshairs(metrics);
  }

  private void slideMap(RelativeLayout layout, SheetSlideMetrics metrics) {
    toolbarWrapper =
      toolbarWrapper == null ? activity.findViewById(R.id.toolbar_wrapper) : toolbarWrapper;
    map = map == null ? layout.findViewById(R.id.map) : map;
    float visibleToolbarHeight =
      Math.max(toolbarWrapper.getHeight() + toolbarWrapper.getTranslationY(), 0f);
    float bottomOffset = Math.min(metrics.getPeekHeight(), metrics.getVisibleHeight());
    float offset = Math.max(bottomOffset - visibleToolbarHeight, 0f);
    layout.setTranslationY(-offset / 2.0f);
  }

  private void fadeCrosshairs(SheetSlideMetrics metrics) {
    crosshairs = crosshairs == null ? activity.findViewById(R.id.map_crosshairs) : crosshairs;
    metrics.hideWithSheet(crosshairs, SHOW_CROSSHAIRS_THRESHOLD, HIDE_CROSSHAIRS_THRESHOLD);
  }
}
