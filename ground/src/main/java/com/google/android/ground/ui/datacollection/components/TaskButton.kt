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
import com.google.android.material.button.MaterialButton

class TaskButton(val view: View) {

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
  when (theme) {
    Theme.LIGHT_GREEN ->
      TaskChipButtonWithIconLightGreenBinding.inflate(layoutInflater, container).button
    else -> error("Unsupported icon type button for theme: $theme")
  }

// TODO(Shobhit): Figure out a way to create styled buttons without using XML.
private fun createTextIconTypeButtons(
  layoutInflater: LayoutInflater,
  container: ViewGroup,
  theme: Theme
): MaterialButton =
  when (theme) {
    Theme.TRANSPARENT ->
      TaskChipButtonWithTextAndIconTransparentBinding.inflate(layoutInflater, container).button
    else -> error("Unsupported icon type button for theme: $theme")
  }

enum class Type {
  TEXT,
  ICON,
  TEXT_ICON,
}

enum class Theme {
  DARK_GREEN,
  LIGHT_GREEN,
  TRANSPARENT,
}

enum class ButtonAction(val type: Type, val theme: Theme) {
  // All tasks
  CONTINUE(Type.TEXT, Theme.DARK_GREEN),
  SKIP(Type.TEXT, Theme.LIGHT_GREEN),
  UNDO(Type.ICON, Theme.LIGHT_GREEN),

  // Drop a pin task
  DROP_PIN(Type.TEXT, Theme.TRANSPARENT),

  // Draw a polygon task
  ADD_PIN(Type.TEXT_ICON, Theme.TRANSPARENT),
  COMPLETE(Type.TEXT, Theme.TRANSPARENT),
}
