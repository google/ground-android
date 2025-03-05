/*
 * Copyright 2024 Google LLC
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

package org.groundplatform.android.ui.home.mapcontainer

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Provides equal spacing between recycler view items in portrait mode. Otherwise, uses the given
 * [defaultSpacing].
 */
class AdaptiveSpacingItemDecorator(resources: Resources, private val defaultSpacing: Int) :
  RecyclerView.ItemDecoration() {

  private val isPortraitMode =
    resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State,
  ) {
    val position = parent.getChildAdapterPosition(view)
    val itemCount = state.itemCount

    // Skip last item
    if (position == itemCount - 1) {
      return
    }

    outRect.right =
      if (isPortraitMode) calculateSpacing(view, parent, itemCount) else defaultSpacing
  }

  /** Calculates the spacing between items based on available empty space. */
  private fun calculateSpacing(view: View, parent: RecyclerView, itemCount: Int): Int {
    // Since the view hasn't gone through the layout phase, we need to first call measure()
    view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    val cardWidth: Int = view.measuredWidth

    val totalWidth = parent.measuredWidth
    val totalSpace = totalWidth - cardWidth * itemCount
    return totalSpace / (itemCount - 1)
  }
}
