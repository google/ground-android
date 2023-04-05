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
import androidx.core.content.res.ResourcesCompat
import com.google.android.ground.databinding.*
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.ui.datacollection.components.ButtonAction.Theme
import com.google.android.ground.ui.datacollection.components.ButtonAction.Type
import com.google.android.material.button.MaterialButton

/** Wrapper class for holding a button. */
data class TaskButton(private val view: View) {

  private var taskUpdatedCallback: ((button: TaskButton, taskData: TaskData?) -> Unit)? = null

  init {
    view.id = View.generateViewId()
  }

  /** Updates the state of the button. */
  fun updateState(block: View.() -> Unit): TaskButton {
    block(view)
    return this
  }

  /** Register a callback to be invoked when this view is clicked. */
  fun setOnClickListener(block: () -> Unit): TaskButton {
    view.setOnClickListener { block() }
    return this
  }

  /** Register a callback to be invoked when [TaskData] is updated. */
  fun setOnTaskUpdated(block: (button: TaskButton, taskData: TaskData?) -> Unit): TaskButton {
    this.taskUpdatedCallback = block
    return this
  }

  /** Must be called when a new [TaskData] is available. */
  fun onTaskDataUpdated(taskData: TaskData?) {
    taskUpdatedCallback?.let { it(this, taskData) }
  }

  companion object {
    /** Inflates the button layout and attaches to the given container view. */
    fun createAndAttachButton(
      action: ButtonAction,
      container: ViewGroup,
      layoutInflater: LayoutInflater
    ) =
      TaskButton(
        when (action.type) {
          Type.TEXT -> createTextTypeButtons(layoutInflater, container, action)
          Type.ICON -> createIconTypeButtons(layoutInflater, container, action)
          Type.TEXT_ICON -> createTextIconTypeButtons(layoutInflater, container, action)
        }
      )
  }
}

// TODO(Shobhit): Figure out a way to create styled buttons without using XML.
private fun createTextTypeButtons(
  layoutInflater: LayoutInflater,
  container: ViewGroup,
  action: ButtonAction
): Button =
  when (action.theme) {
    Theme.DARK_GREEN -> TaskChipButtonDarkGreenBinding.inflate(layoutInflater, container).button
    Theme.LIGHT_GREEN -> TaskChipButtonLightGreenBinding.inflate(layoutInflater, container).button
    Theme.OUTLINED -> TaskChipButtonOutlineBinding.inflate(layoutInflater, container).button
    Theme.TRANSPARENT -> TaskChipButtonTransparentBinding.inflate(layoutInflater, container).button
  }.apply { action.textId?.let { setText(it) } }

// TODO(Shobhit): Figure out a way to create styled buttons without using XML.
private fun createIconTypeButtons(
  layoutInflater: LayoutInflater,
  container: ViewGroup,
  action: ButtonAction
): ImageButton =
  if (action.theme == Theme.LIGHT_GREEN) {
      TaskChipButtonWithIconLightGreenBinding.inflate(layoutInflater, container).button
    } else {
      error("Unsupported icon type button for theme: ${action.theme}")
    }
    .apply {
      action.drawableId?.let { setImageDrawable(ResourcesCompat.getDrawable(resources, it, null)) }
    }

// TODO(Shobhit): Figure out a way to create styled buttons without using XML.
private fun createTextIconTypeButtons(
  layoutInflater: LayoutInflater,
  container: ViewGroup,
  action: ButtonAction
): MaterialButton =
  if (action.theme == Theme.OUTLINED) {
      TaskChipButtonWithTextAndIconTransparentBinding.inflate(layoutInflater, container).button
    } else {
      error("Unsupported icon type button for theme: $action.theme")
    }
    .apply {
      action.textId?.let { setText(it) }
      action.drawableId?.let { icon = ResourcesCompat.getDrawable(resources, it, null) }
    }
