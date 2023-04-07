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
package com.google.android.ground.ui.datacollection.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnAttach
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.google.android.ground.R
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskButton
import com.google.android.ground.ui.datacollection.components.TaskView
import java.util.*
import kotlin.properties.Delegates

abstract class AbstractTaskFragment<T : AbstractTaskViewModel> : AbstractFragment() {

  protected val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)

  private val buttons: EnumMap<ButtonAction, TaskButton> = EnumMap(ButtonAction::class.java)
  private lateinit var taskView: TaskView
  protected lateinit var viewModel: T

  /** Position of the task in the Job's sorted task list. Used for instantiating the [viewModel]. */
  var position by Delegates.notNull<Int>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      position = savedInstanceState.getInt(POSITION)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(POSITION, position)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    taskView = onCreateTaskView(inflater, container)
    return taskView.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.doOnAttach {
      @Suppress("UNCHECKED_CAST")
      viewModel = dataCollectionViewModel.getTaskViewModel(position) as T

      taskView.bind(this, viewModel)
      taskView.addTaskView(onCreateTaskBody(layoutInflater))
      onTaskViewAttached()

      // Add actions buttons after the view model is bound to the view.
      onCreateActionButtons()
      onActionButtonsCreated()
    }
  }

  /** Creates the view for common task template with/without header. */
  abstract fun onCreateTaskView(inflater: LayoutInflater, container: ViewGroup?): TaskView

  /** Creates the view for body of the task. */
  abstract fun onCreateTaskBody(inflater: LayoutInflater): View

  /** Invoked after the task view gets attached to the fragment. */
  open fun onTaskViewAttached() {}

  /** Invoked when the fragment is ready to add buttons to the current [TaskView]. */
  open fun onCreateActionButtons() {
    addContinueButton()
    addSkipButton()
  }

  /** Invoked when the all [ButtonAction]s are added to the current [TaskView]. */
  open fun onActionButtonsCreated() {
    viewModel.taskData.observe(viewLifecycleOwner) { onTaskDataUpdated(it.orElse(null)) }
  }

  /** Invoked when the data associated with the current task gets modified. */
  protected open fun onTaskDataUpdated(taskData: TaskData?) {
    for ((_, button) in buttons) {
      button.onTaskDataUpdated(taskData)
    }
  }

  protected fun addContinueButton() {
    addButton(ButtonAction.CONTINUE)
      .setOnClickListener { dataCollectionViewModel.onContinueClicked() }
      .setOnTaskUpdated { button, taskData ->
        button.updateState { isEnabled = taskData.isNotNullOrEmpty() }
      }
      .updateState { isEnabled = false }
  }

  protected fun addSkipButton() {
    addButton(ButtonAction.SKIP)
      .setOnClickListener {
        viewModel.clearResponse()
        dataCollectionViewModel.onContinueClicked()
      }
      .updateState { visibility = if (viewModel.isTaskOptional()) View.VISIBLE else View.GONE }
  }

  fun addUndoButton() {
    addButton(ButtonAction.UNDO)
      .setOnClickListener { viewModel.clearResponse() }
      .setOnTaskUpdated { button, taskData ->
        button.updateState {
          visibility = if (taskData.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
      }
      .updateState {
        visibility = View.GONE
        isEnabled = true
      }
  }

  protected fun addButton(action: ButtonAction): TaskButton {
    check(!buttons.contains(action)) { "Button $action already bound" }
    val button =
      TaskButton.createAndAttachButton(action, taskView.actionButtonsContainer, layoutInflater)
    buttons[action] = button
    return button
  }

  protected fun getButton(action: ButtonAction): TaskButton {
    check(buttons.contains(action)) { "Expected key $action in $buttons" }
    return buttons[action]!!
  }

  companion object {
    /** Key used to store the position of the task in the Job's sorted tasklist. */
    const val POSITION = "position"
  }
}
