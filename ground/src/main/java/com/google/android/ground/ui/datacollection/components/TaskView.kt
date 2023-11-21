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
import androidx.fragment.app.Fragment
import com.google.android.ground.databinding.TaskFragActionButtonsBinding
import com.google.android.ground.databinding.TaskFragWithCombinedHeaderBinding
import com.google.android.ground.databinding.TaskFragWithHeaderBinding
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel

/** Wrapper class for holding entire task's view (except toolbar). */
sealed interface TaskView {

  /** Container for adding the action buttons for the task. */
  val actionButtonsContainer: TaskFragActionButtonsBinding

  /** Root-level view for the current task. */
  val root: View

  /** Binds the given parameters to the view binding. Should only be called once. */
  fun bind(fragment: Fragment, viewModel: AbstractTaskViewModel)

  /** Adds given view as the body of current the task. Should only be called once. */
  fun addTaskView(view: View)
}

/** Implementation of [TaskView] with an embedded header. */
data class TaskViewWithHeader(private val binding: TaskFragWithHeaderBinding) : TaskView {

  override val actionButtonsContainer = binding.actionButtons

  override val root = binding.root

  override fun bind(fragment: Fragment, viewModel: AbstractTaskViewModel) {
    binding.viewModel = viewModel
    binding.lifecycleOwner = fragment
  }

  override fun addTaskView(view: View) {
    binding.taskContainer.addView(view)
  }
}

/** Implementation of [TaskView] with a header that is an extension of the title bar. */
data class TaskViewWithCombinedHeader(private val binding: TaskFragWithCombinedHeaderBinding) :
  TaskView {

  override val actionButtonsContainer = binding.actionButtons

  override val root = binding.root

  override fun bind(fragment: Fragment, viewModel: AbstractTaskViewModel) {
    binding.viewModel = viewModel
    binding.lifecycleOwner = fragment
  }

  override fun addTaskView(view: View) {
    binding.taskContainer.addView(view)
  }
}
