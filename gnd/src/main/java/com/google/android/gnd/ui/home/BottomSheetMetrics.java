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

import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.common.BottomSheetBehavior;


/** Abstracts access to dimensions and positions of elements relative to the bottom sheet UI. */
public class BottomSheetMetrics {
  private final CoordinatorLayout parent;
  private final View bottomSheet;
  private final View addObservationButton;
  private final BottomSheetBehavior<View> bottomSheetBehavior;
  private final View header;
  private final View toolbarWrapper;
  private final int marginTop;

  BottomSheetMetrics(View bottomSheet) {
    this.parent = (CoordinatorLayout) bottomSheet.getParent();
    this.bottomSheet = bottomSheet;
    this.addObservationButton = parent.findViewById(R.id.add_observation_btn);
    this.bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
    this.header = parent.findViewById(R.id.bottom_sheet_header);
    this.toolbarWrapper = parent.findViewById(R.id.toolbar_wrapper);
    this.marginTop = (int) parent.getResources().getDimension(R.dimen.bottom_sheet_margin_top);
  }

  /** Returns the number of pixels of the bottom sheet visible above the bottom of the screen. */
  public int getVisibleHeight() {
    return Math.max(parent.getHeight() - bottomSheet.getTop(), 0);
  }

  /**
   * Returns a ratio indicating bottom sheet scroll progress from hidden to visible state.
   * Specifically, it returns 0 when the bottom sheet is fully hidden, 1 when the top of the
   * observation list just passes the top of the "Add Observation" button, and a linearly
   * interpolated ratio for all values in between.
   */
  public float getRevealRatio() {
    float buttonDistanceFromBottom =
        Math.max(parent.getHeight() - addObservationButton.getTop(), 0);
    float sheetBodyVisibleHeight = getVisibleHeight() - header.getHeight();
    return Math.min(sheetBodyVisibleHeight / buttonDistanceFromBottom, 1.0f);
  }

  /**
   * Returns the "peek height" of the bottom sheet, the height of the sheet when it is initially
   * displayed and to which it snaps in "collapsed" state between full expanded and fully hidden.
   */
  public int getPeekHeight() {
    return bottomSheetBehavior.getPeekHeight();
  }

  /**
   * Returns the number of pixels the sheet has been expanded above peek height, or 0 if it is
   * currently positioned below peek height.
   */
  public int getExpansionHeight() {
    return Math.max(getVisibleHeight() - bottomSheetBehavior.getPeekHeight(), 0);
  }

  /**
   * Calculates the expected height of the bottom sheet when fully expanded, assuming the sheet will
   * stop expanding just below the top toolbar.
   */
  public int getExpandedOffset() {
    return toolbarWrapper.getHeight() - marginTop;
  }

  /**
   * Returns bottom sheet slide progress as the linearly interpolated value between 0 (the bottom
   * sheet is scrolled to peek height) and 1 (the bottom sheet is fully expanded).
   */
  public float getExpansionRatio() {
    // Bottom sheet top position relative to its fully expanded state (0=full expanded).
    float relativeTop = bottomSheet.getTop() - getExpandedOffset();

    // The relative top position when the bottom sheet is in "collapsed" position (i.e., open to
    // the bottom sheet peek height).
    float relativePeekTop =
        parent.getHeight() - bottomSheetBehavior.getPeekHeight() - getExpandedOffset();
    return Math.max(1.0f - (relativeTop / relativePeekTop), 0f);
  }
}
