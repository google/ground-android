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
package com.google.android.ground.ui.home.mapcontainer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.ground.R
import com.google.android.ground.ui.home.BottomSheetDependentBehavior
import com.google.android.ground.ui.home.BottomSheetMetrics

class MapContainerLayoutBehavior(context: Context, attrs: AttributeSet) :
  BottomSheetDependentBehavior<FrameLayout>(context, attrs) {
  override fun onBottomSheetChanged(
    parent: CoordinatorLayout,
    mapContainerLayout: FrameLayout,
    metrics: BottomSheetMetrics
  ) {
    if (metrics.peekHeight <= 0) {
      return
    }
    val map = mapContainerLayout.findViewById<View>(R.id.map)
    val mapControls = mapContainerLayout.findViewById<View>(R.id.map_controls)
    if (map == null || mapControls == null) {
      // View already destroyed.
      return
    }

    // always keep the map centered based on the visible portion of map (excluding status bar)
    if (metrics.visibleHeight >= metrics.expandedOffset) {
      val translationY = -(metrics.visibleHeight - metrics.expandedOffset) / 2
      map.translationY = translationY.toFloat()
    } else {
      map.translationY = 0f
    }

    val hideRatio = 1.0f - metrics.revealRatio
    mapControls.alpha = hideRatio
  }
}
