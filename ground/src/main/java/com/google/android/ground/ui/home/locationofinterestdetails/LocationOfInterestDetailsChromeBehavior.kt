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
package com.google.android.ground.ui.home.locationofinterestdetails

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.ground.R
import com.google.android.ground.ui.home.BottomSheetDependentBehavior
import com.google.android.ground.ui.home.BottomSheetMetrics
import timber.log.Timber

/**
 * Defines behavior of the LOI details UI elements (bottom sheet and chrome) when the bottom sheet
 * is scrolled, collapsed, or expanded.
 */
class LocationOfInterestDetailsChromeBehavior(context: Context, attrs: AttributeSet) :
  BottomSheetDependentBehavior<ViewGroup>(context, attrs) {
  override fun onBottomSheetChanged(
    parent: CoordinatorLayout,
    child: ViewGroup,
    metrics: BottomSheetMetrics
  ) {
    Timber.d("onBottomSheetChanged")
    val toolbarWrapper = child.findViewById<ViewGroup>(R.id.toolbar_wrapper)
    val bottomSheetScrim = child.findViewById<View>(R.id.bottom_sheet_bottom_inset_scrim)
    val hamburgerButton = parent.findViewById<View>(R.id.hamburger_btn)
    val mapScrim = parent.findViewById<View>(R.id.map_scrim)

    // Fade in the bottom scrim and "Add Submission" button, with both being fully visible as soon
    // as the top of the bottom sheet passes the top of the "Add Submission" button.
    val revealRatio = metrics.revealRatio
    val hideRatio = 1.0f - revealRatio
    child.alpha = revealRatio
    mapScrim.alpha = metrics.expansionRatio
    bottomSheetScrim.alpha = revealRatio
    toolbarWrapper.alpha = revealRatio
    toolbarWrapper.translationY = -toolbarWrapper.height * hideRatio
    hamburgerButton.alpha = hideRatio
  }
}
