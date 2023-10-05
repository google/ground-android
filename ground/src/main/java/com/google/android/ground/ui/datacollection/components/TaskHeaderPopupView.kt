/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.datacollection.components

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import com.google.android.ground.databinding.PopupLayoutBinding

/** PopupView for displaying label for tasks. */
class TaskHeaderPopupView(val context: Context) {
  private var binding: PopupLayoutBinding
  private var window: PopupWindow

  init {
    val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    binding = PopupLayoutBinding.inflate(layoutInflater)
    window = createPopupWindow(context, binding.root)
  }

  fun show(anchor: View, text: String?) {
    val anchorRect = getAnchorRect(anchor)
    val xPos = anchorRect.centerX() - getRootWidth() / 2
    val yPos = anchorRect.bottom
    window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos)
    binding.textView.text = text
  }

  private fun getAnchorRect(anchor: View): Rect {
    val location = IntArray(2)
    anchor.getLocationOnScreen(location)
    val left = location[0]
    val right = left + anchor.width
    val top = location[1]
    val bottom = top + anchor.height
    return Rect(left, top, right, bottom)
  }

  private fun getRootWidth(): Int =
    binding.root
      .apply { measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
      .measuredWidth

  private fun createPopupWindow(context: Context, view: View): PopupWindow =
    PopupWindow(context).apply {
      setBackgroundDrawable(BitmapDrawable())
      width = WindowManager.LayoutParams.WRAP_CONTENT
      height = WindowManager.LayoutParams.WRAP_CONTENT
      isTouchable = true
      isFocusable = true
      isOutsideTouchable = true
      contentView = view
    }
}
