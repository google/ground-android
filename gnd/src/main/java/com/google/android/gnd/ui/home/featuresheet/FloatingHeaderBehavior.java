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

package com.google.android.gnd.ui.home.featuresheet;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.common.BottomSheetBehavior;

/**
 * Custom behavior to manage position and alpha of floating header, and to link movements of
 * BottomSheetBehavior with header.
 */
public class FloatingHeaderBehavior extends CoordinatorLayout.Behavior {
  public FloatingHeaderBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onLayoutChild(
      @NonNull CoordinatorLayout parent, @NonNull View header, int layoutDirection) {
    // Propagate touch events from the header to the bottom sheet so that dragging the header
    // will also scroll the bottom sheet. We do this here instead of in the view so that this
    // behavior can be self-contained.
    for (int i = 0; i < parent.getChildCount(); i++) {
      View view = parent.getChildAt(i);
      if (layoutDependsOn(parent, header, parent.getChildAt(i))) {
        header.setOnTouchListener((v, ev) -> view.dispatchTouchEvent(ev));
      }
    }

    // Setting here since setting background in layout xml has no effect.
    header.setBackgroundResource(R.drawable.bg_header_card);

    // Required to allow icon to float above header.
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      ((CardView) header).setClipToOutline(false);
    }
    return false;
  }

  @Override
  public boolean layoutDependsOn(
      @NonNull CoordinatorLayout parent, @NonNull View header, @NonNull View dependency) {
    ViewGroup.LayoutParams params = dependency.getLayoutParams();
    return params instanceof CoordinatorLayout.LayoutParams
        && ((CoordinatorLayout.LayoutParams) params).getBehavior() instanceof BottomSheetBehavior;
  }

  @Override
  public boolean onDependentViewChanged(
      @NonNull CoordinatorLayout parent, @NonNull View header, @NonNull View bottomSheet) {

    float sheetY = bottomSheet.getY();
    float sheetHeight = Math.max(parent.getHeight() - bottomSheet.getY(), 0);
    MarginLayoutParams lp = (MarginLayoutParams) header.getLayoutParams();
    int headerHeight = header.getHeight() + lp.topMargin + lp.bottomMargin;

    // Scroll the header together with the bottom sheet. This must be done programmatically
    // because CoordinatorLayout anchors cause bottom sheet height to expand, preventing header
    // from disappearing behind toolbar.
    header.setTranslationY(sheetY - headerHeight);

    // Fade out header and bottom sheet starting when bottom sheet is the same height as the
    // header to when it is completely hidden. This prevents the header from sticking out above
    // the bottom of the screen when the bottom sheet is in hidden state.
    float alpha = Math.min(sheetHeight / headerHeight, 1.0f);
    header.setAlpha(alpha);
    bottomSheet.setAlpha(alpha);

    // Show/hide the header based on whether the bottom sheet is visible.
    header.setVisibility(sheetHeight == 0 ? View.GONE : View.VISIBLE);

    return true;
  }
}
