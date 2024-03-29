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

import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.google.android.ground.model.submission.Value

class TaskButton {

  private lateinit var clickCallback: () -> Unit

  private var enabled: MutableState<Boolean> = mutableStateOf(true)
  private var hidden: MutableState<Boolean> = mutableStateOf(false)

  private var taskUpdatedCallback: ((button: TaskButton, value: Value?) -> Unit)? = null

  @Composable
  fun CreateButton(action: ButtonAction) {
    if (!hidden.value) {
      when (action.theme) {
        ButtonAction.Theme.DARK_GREEN ->
          Button(onClick = { clickCallback() }, enabled = enabled.value) { Content(action) }
        ButtonAction.Theme.LIGHT_GREEN ->
          FilledTonalButton(onClick = { clickCallback() }, enabled = enabled.value) {
            Content(action)
          }
        ButtonAction.Theme.OUTLINED ->
          OutlinedButton(onClick = { clickCallback() }, enabled = enabled.value) { Content(action) }
        ButtonAction.Theme.TRANSPARENT ->
          OutlinedButton(border = null, onClick = { clickCallback() }, enabled = enabled.value) {
            Content(action)
          }
      }
    }
  }

  @Composable
  private fun Content(action: ButtonAction) {
    // Icon
    action.drawableId?.let {
      Icon(imageVector = ImageVector.vectorResource(id = it), contentDescription = "")
    }

    // Label
    action.textId?.let { textId -> Text(text = stringResource(id = textId)) }
  }

  /** Updates the `visibility` property button. */
  fun showIfTrue(result: Boolean): TaskButton = if (result) show() else hide()

  /** Updates the `isEnabled` property of button. */
  fun enableIfTrue(result: Boolean): TaskButton = if (result) enable() else disable()

  fun show(): TaskButton {
    hidden.value = false
    return this
  }

  fun hide(): TaskButton {
    hidden.value = true
    return this
  }

  fun enable(): TaskButton {
    enabled.value = true
    return this
  }

  fun disable(): TaskButton {
    enabled.value = false
    return this
  }

  /** Register a callback to be invoked when this view is clicked. */
  fun setOnClickListener(block: () -> Unit): TaskButton {
    this.clickCallback = block
    return this
  }

  /** Register a callback to be invoked when [Value] is updated. */
  fun setOnValueChanged(block: (button: TaskButton, value: Value?) -> Unit): TaskButton {
    this.taskUpdatedCallback = block
    return this
  }

  /** Must be called when a new [Value] is available. */
  fun onValueChanged(value: Value?) {
    taskUpdatedCallback?.let { it(this, value) }
  }
}
