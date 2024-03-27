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
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.core.content.res.ResourcesCompat
import com.google.android.ground.databinding.*
import com.google.android.material.button.MaterialButton

// TODO(Shobhit): Figure out a way to create styled buttons without using XML.
/**
 * Factory for creating a [TaskButton] for the given [ButtonAction] and attaching it to the given
 * container.
 */
object TaskButtonFactory {

  /** Inflates the button layout and attaches to the given container view. */
  fun createAndAttachButton(
    action: ButtonAction,
    container: ViewGroup,
    layoutInflater: LayoutInflater
  ) =
    TaskButton(
      when (action.type) {
        ButtonAction.Type.TEXT -> createTextTypeButtons(layoutInflater, container, action)
        ButtonAction.Type.ICON -> createIconTypeButtons(layoutInflater, container, action)
        ButtonAction.Type.TEXT_ICON -> createTextIconTypeButtons(layoutInflater, container, action)
      }
    )

  private fun createTextTypeButtons(
    layoutInflater: LayoutInflater,
    container: ViewGroup,
    action: ButtonAction
  ): Button =
    when (action.theme) {
      ButtonAction.Theme.DARK_GREEN ->
        TaskChipButtonDarkGreenBinding.inflate(layoutInflater, container).button

      ButtonAction.Theme.LIGHT_GREEN ->
        TaskChipButtonLightGreenBinding.inflate(layoutInflater, container).button

      ButtonAction.Theme.OUTLINED ->
        TaskChipButtonOutlineBinding.inflate(layoutInflater, container).button

      ButtonAction.Theme.TRANSPARENT ->
        TaskChipButtonTransparentBinding.inflate(layoutInflater, container).button
    }.apply { action.textId?.let { setText(it) } }

  private fun createIconTypeButtons(
    layoutInflater: LayoutInflater,
    container: ViewGroup,
    action: ButtonAction
  ): ImageButton =
    if (action.theme == ButtonAction.Theme.LIGHT_GREEN) {
      TaskChipButtonWithIconLightGreenBinding.inflate(layoutInflater, container).button
    } else {
      error("Unsupported icon type button for theme: ${action.theme}")
    }
      .apply {
        action.drawableId?.let {
          setImageDrawable(ResourcesCompat.getDrawable(resources, it, null))
        }
      }

  private fun createTextIconTypeButtons(
    layoutInflater: LayoutInflater,
    container: ViewGroup,
    action: ButtonAction
  ): MaterialButton =
    if (action.theme == ButtonAction.Theme.OUTLINED) {
      TaskChipButtonWithTextAndIconTransparentBinding.inflate(layoutInflater, container).button
    } else {
      error("Unsupported icon type button for theme: $action.theme")
    }
      .apply {
        action.textId?.let { setText(it) }
        action.drawableId?.let { icon = ResourcesCompat.getDrawable(resources, it, null) }
      }
}
