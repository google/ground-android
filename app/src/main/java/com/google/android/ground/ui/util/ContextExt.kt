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

package com.google.android.ground.ui.util

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Paint
import android.os.Build
import android.text.TextPaint
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat

/**
 * Returns an instance of [TextPaint] populated using the font, size, and color of the specified
 * style.
 */
fun Context.obtainTextPaintFromStyle(@StyleRes styleRes: Int): TextPaint {
  fun <T> withStyledAttribute(@AttrRes attrRes: Int, fn: (typedArray: TypedArray) -> T) {
    val typedArray: TypedArray = obtainStyledAttributes(styleRes, intArrayOf(attrRes))
    try {
      fn(typedArray)
    } finally {
      typedArray.recycle()
    }
  }

  val textPaint = TextPaint(Paint())
  withStyledAttribute(android.R.attr.textSize) {
    textPaint.textSize = it.getDimensionPixelSize(0, 0).toFloat()
  }
  withStyledAttribute(android.R.attr.textColor) { textPaint.color = it.getColor(0, 0) }
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    withStyledAttribute(android.R.attr.fontFamily) {
      textPaint.typeface = ResourcesCompat.getFont(this, it.getResourceId(0, 0))
    }
  }
  return textPaint
}
