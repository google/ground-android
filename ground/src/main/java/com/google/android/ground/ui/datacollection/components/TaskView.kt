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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import com.google.android.ground.databinding.TaskFragWithHeaderBinding
import com.google.android.ground.databinding.TaskFragWithoutHeaderBinding
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel

/** Wrapper class for holding entire task's view (except toolbar). */
sealed interface TaskView {

  /** Container for adding the action buttons for the task. */
  val actionButtonsContainer: ViewGroup

  /** Root-level view for the current task. */
  val root: View

  /** Binds the given parameters to the view binding. Should only be called once. */
  fun bind(fragment: Fragment, viewModel: AbstractTaskViewModel)

  /** Adds given view as the body of current the task. Should only be called once. */
  fun addTaskView(view: View)
}

/** Implementation of [TaskView] with an embedded header. */
data class TaskViewWithHeader(private val binding: TaskFragWithHeaderBinding) : TaskView {

  override val actionButtonsContainer = binding.actionButtonsContainer

  override val root = binding.root

  override fun bind(fragment: Fragment, viewModel: AbstractTaskViewModel) {
    binding.viewModel = viewModel
    binding.lifecycleOwner = fragment
  }

  override fun addTaskView(view: View) {
    binding.taskContainer.addView(view)
  }

  companion object {
    fun create(layoutInflater: LayoutInflater): TaskView {
      val binding = TaskFragWithHeaderBinding.inflate(layoutInflater)
      return TaskViewWithHeader(binding)
    }
  }
}

/** Implementation of [TaskView] without an embedded header. */
data class TaskViewWithoutHeader(private val binding: TaskFragWithoutHeaderBinding) : TaskView {

  override val actionButtonsContainer = binding.actionButtonsContainer

  override val root = binding.root

  override fun bind(fragment: Fragment, viewModel: AbstractTaskViewModel) {
    binding.viewModel = viewModel
    binding.lifecycleOwner = fragment
  }

  override fun addTaskView(view: View) {
    binding.taskContainer.addView(view)
  }

  companion object {
    fun create(
      layoutInflater: LayoutInflater,
      @DrawableRes iconResId: Int? = null,
      @StringRes labelResId: Int? = null
    ): TaskView {
      val binding = TaskFragWithoutHeaderBinding.inflate(layoutInflater)
      iconResId?.let {
        val drawable = AppCompatResources.getDrawable(layoutInflater.context, it)
        binding.headerIcon.setImageDrawable(drawable)
      }
      labelResId?.let { binding.headerLabel.setText(labelResId) }
      return TaskViewWithoutHeader(binding)
    }
  }
}
