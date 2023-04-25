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

import android.view.View
import com.google.android.ground.model.submission.TaskData
import org.jetbrains.annotations.TestOnly

/** Wrapper class for holding a button. */
data class TaskButton(private val view: View) {

  private var taskUpdatedCallback: ((button: TaskButton, taskData: TaskData?) -> Unit)? = null

  init {
    view.id = View.generateViewId()
  }

  /** Updates the `visibility` property button. */
  fun showIfTrue(result: Boolean): TaskButton = if (result) show() else hide()

  /** Updates the `isEnabled` property of button. */
  fun enableIfTrue(result: Boolean): TaskButton = if (result) enable() else disable()

  fun show(): TaskButton {
    view.visibility = View.VISIBLE
    return this
  }

  fun hide(): TaskButton {
    view.visibility = View.GONE
    return this
  }

  fun enable(): TaskButton {
    view.isEnabled = true
    return this
  }

  fun disable(): TaskButton {
    view.isEnabled = false
    return this
  }

  @TestOnly fun getView(): View = view

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
}
