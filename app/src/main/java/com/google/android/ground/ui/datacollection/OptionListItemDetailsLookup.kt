/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.datacollection

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.widget.RecyclerView

/** Provides [ItemDetails] for the Multiple Choice option items. */
class OptionListItemDetailsLookup(
  val recyclerView: RecyclerView,
) : ItemDetailsLookup<Long>() {
  override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
    val view: View = recyclerView.findChildViewUnder(e.x, e.y) ?: return null

    val holder: RecyclerView.ViewHolder = recyclerView.getChildViewHolder(view)

    val position = holder.adapterPosition
    return object : ItemDetails<Long>() {
      override fun getPosition(): Int = position

      override fun getSelectionKey(): Long = position.toLong()
    }
  }
}
