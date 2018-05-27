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

package com.google.android.gnd.ui.placesheet;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.OnSheetSlideBehavior;

public class PlaceSheetChromeBehavior extends OnSheetSlideBehavior<ViewGroup> {
  // TODO: Refactor transitions into "TransitionEffect" classes.
  private static final float HIDE_SCRIM_THRESHOLD = 0.0f;
  private static final float SHOW_SCRIM_THRESHOLD = 0.1f;
  private static final float HIDE_ADD_BUTTON_THRESHOLD = 0.3f;
  private static final float SHOW_ADD_BUTTON_THRESHOLD = 0.5f;

  public PlaceSheetChromeBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onSheetScrolled(
    CoordinatorLayout parent, ViewGroup layout, SheetSlideMetrics metrics) {
    View scrim = layout.findViewById(R.id.bottom_sheet_bottom_inset_scrim);
    View addRecordButton = layout.findViewById(R.id.add_record_btn);
    metrics.showWithSheet(scrim, HIDE_SCRIM_THRESHOLD, SHOW_SCRIM_THRESHOLD);
    metrics.showWithSheet(addRecordButton, HIDE_ADD_BUTTON_THRESHOLD, SHOW_ADD_BUTTON_THRESHOLD);
  }
}
