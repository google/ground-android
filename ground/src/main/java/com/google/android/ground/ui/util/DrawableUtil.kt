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
package com.google.android.ground.ui.util

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.color.MaterialColors
import javax.inject.Inject

class DrawableUtil @Inject constructor(private val resources: Resources) {

  @ColorInt fun getColor(@ColorRes colorId: Int): Int = resources.getColor(colorId)

  private fun getDrawable(@DrawableRes drawableId: Int): Drawable? =
    ResourcesCompat.getDrawable(resources, drawableId, null)

  fun getDrawable(view: View, @DrawableRes drawableId: Int, @AttrRes colorId: Int): Drawable? {
    val drawable = getDrawable(drawableId)
    drawable?.setColorFilter(MaterialColors.getColor(view, colorId), PorterDuff.Mode.SRC_ATOP)
    return drawable
  }
}
