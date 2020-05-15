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
import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.gnd.R;

/**
 * Base class for layout behaviors defining transitions dependent on bottom sheet changes (e.g.,
 * scroll, expand, collapse).
 */
public abstract class BottomSheetDependentBehavior<V extends View>
    extends CoordinatorLayout.Behavior<V> {
  public BottomSheetDependentBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Overridden to define the behavior of layouts in relation to changes in the bottom sheet.
   * In general, this is called when the bottom sheet state changes (e.g., from hidden to
   * collapsed), and when the bottom sheet is scrolled up or down.
   */
  protected abstract void onBottomSheetChanged(
      CoordinatorLayout parent, V child, BottomSheetMetrics metrics);

  @Override
  public boolean layoutDependsOn(CoordinatorLayout parent, V child, View dependency) {
    return dependency.getId() == R.id.bottom_sheet_layout;
  }

  @Override
  public boolean onDependentViewChanged(CoordinatorLayout parent, V child, View bottomSheet) {
    onBottomSheetChanged(parent, child, new BottomSheetMetrics(parent, bottomSheet));
    return false;
  }
}
