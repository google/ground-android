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
package com.google.android.ground.ui.home

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.ground.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.max
import kotlin.math.min
import timber.log.Timber

/** Abstracts access to dimensions and positions of elements relative to the bottom sheet UI. */
class BottomSheetMetrics internal constructor(bottomSheet: View) {
  private val parent: CoordinatorLayout
  private val bottomSheet: View
  private val addSubmissionButton: View
  private val bottomSheetBehavior: BottomSheetBehavior<View>
  private val header: View
  private val toolbarWrapper: View
  private val marginTop: Int

  /** Returns the number of pixels of the bottom sheet visible above the bottom of the screen. */
  val visibleHeight: Int
    get() = max(parent.height - bottomSheet.top, 0)

  /**
   * Returns a ratio indicating bottom sheet scroll progress from hidden to visible state.
   * Specifically, it returns 0 when the bottom sheet is fully hidden, 1 when the top of the
   * submission list just passes the top of the "Add Submission" button, and a linearly interpolated
   * ratio for all values in between.
   */
  val revealRatio: Float
    get() {
      val buttonDistanceFromBottom = max(parent.height - addSubmissionButton.top, 0).toFloat()
      val sheetBodyVisibleHeight = (visibleHeight - header.height).toFloat()
      return min(sheetBodyVisibleHeight / buttonDistanceFromBottom, 1.0f)
    }

  /**
   * Returns the "peek height" of the bottom sheet, the height of the sheet when it is initially
   * displayed and to which it snaps in "collapsed" state between full expanded and fully hidden.
   */
  val peekHeight: Int
    // TODO(#828): Remove this workaround once the root cause is identified and fixed.
    get() = bottomSheetBehavior.peekHeight

  /**
   * Calculates the expected height of the bottom sheet when fully expanded, assuming the sheet will
   * stop expanding just below the top toolbar.
   */
  val expandedOffset: Int
    // TODO(#828): Remove this workaround once the root cause is identified and fixed.
    get() =
      if (toolbarWrapper.height < marginTop) {
        Timber.e(
          "toolbarWrapper height %d < marginTop %d. Falling back to default height",
          toolbarWrapper.height,
          marginTop
        )
        FALLBACK_EXPANDED_OFFSET
      } else {
        toolbarWrapper.height - marginTop
      }

  /**
   * Returns bottom sheet slide progress as the linearly interpolated value between 0 (the bottom
   * sheet is scrolled to peek height) and 1 (the bottom sheet is fully expanded).
   */
  val expansionRatio: Float
    get() {
      // Bottom sheet top position relative to its fully expanded state (0=full expanded).
      val relativeTop = (bottomSheet.top - expandedOffset).toFloat()

      // The relative top position when the bottom sheet is in "collapsed" position (i.e., open to
      // the bottom sheet peek height).
      val relativePeekTop =
        (parent.height - bottomSheetBehavior.peekHeight - expandedOffset).toFloat()
      return max(1.0f - relativeTop / relativePeekTop, 0f)
    }

  companion object {
    /** Fallback toolbar height - margin top used when toolbar height is uninitialized. */
    const val FALLBACK_EXPANDED_OFFSET = 210 - 168
  }

  init {
    this.bottomSheet = bottomSheet
    parent = bottomSheet.parent as CoordinatorLayout
    addSubmissionButton = parent.findViewById(R.id.add_submission_btn)
    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
    header = parent.findViewById(R.id.bottom_sheet_header)
    toolbarWrapper = parent.findViewById(R.id.toolbar_wrapper)
    marginTop = parent.resources.getDimension(R.dimen.bottom_sheet_margin_top).toInt()
  }
}
