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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.ground.databinding.*
import com.google.android.ground.ui.datacollection.components.ButtonAction.Theme
import com.google.android.ground.ui.datacollection.components.ButtonAction.Type
import com.google.android.material.button.MaterialButton

/** Wrapper class for holding a button. */
data class TaskButton(val view: View) {

  companion object {
    /** Inflates the button layout and attaches to the given container view. */
    fun createAndAttachButton(
      action: ButtonAction,
      container: ViewGroup,
      layoutInflater: LayoutInflater,
      @DrawableRes drawableId: Int?,
      @StringRes textId: Int?,
    ) =
      TaskButton(
        when (action.type) {
          Type.TEXT ->
            createTextTypeButtons(layoutInflater, container, action.theme).apply {
              setText(requireNotNull(textId))
            }
          Type.ICON ->
            createIconTypeButtons(layoutInflater, container, action.theme).apply {
              setImageDrawable(
                ResourcesCompat.getDrawable(resources, requireNotNull(drawableId), null)
              )
            }
          Type.TEXT_ICON -> {
            createTextIconTypeButtons(layoutInflater, container, action.theme).apply {
              setText(requireNotNull(textId))
              icon = ResourcesCompat.getDrawable(resources, requireNotNull(drawableId), null)
            }
          }
        }.apply {
          id = View.generateViewId()
          // Default state
          isEnabled = false
        }
      )
  }
}

// TODO(Shobhit): Figure out a way to create styled buttons without using XML.
private fun createTextTypeButtons(
  layoutInflater: LayoutInflater,
  container: ViewGroup,
  theme: Theme
): Button =
  when (theme) {
    Theme.DARK_GREEN -> TaskChipButtonDarkGreenBinding.inflate(layoutInflater, container).button
    Theme.LIGHT_GREEN -> TaskChipButtonLightGreenBinding.inflate(layoutInflater, container).button
    Theme.TRANSPARENT -> TaskChipButtonTransparentBinding.inflate(layoutInflater, container).button
  }

// TODO(Shobhit): Figure out a way to create styled buttons without using XML.
private fun createIconTypeButtons(
  layoutInflater: LayoutInflater,
  container: ViewGroup,
  theme: Theme
): ImageButton =
  if (theme == Theme.LIGHT_GREEN) {
    TaskChipButtonWithIconLightGreenBinding.inflate(layoutInflater, container).button
  } else {
    error("Unsupported icon type button for theme: $theme")
  }

// TODO(Shobhit): Figure out a way to create styled buttons without using XML.
private fun createTextIconTypeButtons(
  layoutInflater: LayoutInflater,
  container: ViewGroup,
  theme: Theme
): MaterialButton =
  if (theme == Theme.TRANSPARENT) {
    TaskChipButtonWithTextAndIconTransparentBinding.inflate(layoutInflater, container).button
  } else {
    error("Unsupported icon type button for theme: $theme")
  }
