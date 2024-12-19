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
package org.groundplatform.ground.util.drwable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

// https://github.com/dbottillo/Blog/blob/espresso_match_imageview/app/src/androidTest/java/com/danielebottillo/blog/config/DrawableMatcher.java
/**
 * This class mainly provides a custom matcher to test whether the drawable-image is correctly shown
 * in ImageView.
 */
private class DrawableMatcher(private val expectedId: Int) :
  TypeSafeMatcher<View>(View::class.java) {

  private var resourceName: String? = null

  override fun matchesSafely(target: View): Boolean {
    if (target !is ImageView) {
      return false
    }
    if (expectedId == NONE) {
      return target.drawable == null
    }
    if (expectedId == ANY) {
      return target.drawable != null
    }
    val resources = target.context.resources
    val expectedDrawable = resources.getDrawable(expectedId, target.context.theme)
    resourceName = resources.getResourceEntryName(expectedId)

    if (expectedDrawable == null) {
      return false
    }

    val targetBitmap = getBitmap(target.drawable)
    val expectedBitmap = getBitmap(expectedDrawable)
    return targetBitmap.sameAs(expectedBitmap)
  }

  private fun getBitmap(drawable: Drawable): Bitmap {
    val targetBitmap =
      Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888,
      )
    val canvas = Canvas(targetBitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return targetBitmap
  }

  override fun describeTo(description: Description) {
    description.appendText("with drawable from resource id: ")
    description.appendValue(expectedId)
    if (resourceName != null) {
      description.appendText("[")
      description.appendText(resourceName)
      description.appendText("]")
    }
  }

  companion object {
    const val NONE = -1
    const val ANY = -2
  }
}

fun withDrawable(resourceId: Int): Matcher<View> = DrawableMatcher(resourceId)
