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
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskButton
import com.google.android.ground.ui.datacollection.components.TaskButtonFactory
import com.google.android.ground.ui.datacollection.components.TaskView
import java.util.EnumMap
import kotlin.properties.Delegates
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly

abstract class AbstractTaskFragment<T : AbstractTaskViewModel> : AbstractFragment() {

  protected val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)

  private val buttons: EnumMap<ButtonAction, TaskButton> = EnumMap(ButtonAction::class.java)
  private val buttonsIndex: MutableMap<Int, ButtonAction> = mutableMapOf()
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
    taskView = onCreateTaskView(inflater)
    return taskView.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.doOnAttach {
      @Suppress("UNCHECKED_CAST", "LabeledExpression")
      val vm = dataCollectionViewModel.getTaskViewModel(position) as? T ?: return@doOnAttach

      viewModel = vm
      taskView.bind(this, viewModel)
      taskView.addTaskView(onCreateTaskBody(layoutInflater))

      // Add actions buttons after the view model is bound to the view.
      addPreviousButton()
      onCreateActionButtons()
      onActionButtonsCreated()

      onTaskViewAttached()
    }
  }

  override fun setMenuVisibility(menuVisible: Boolean) {
    super.setMenuVisibility(menuVisible)
    if (menuVisible) {
      onTaskVisibleToUser()
    }
  }

  /** Creates the view for common task template with/without header. */
  abstract fun onCreateTaskView(inflater: LayoutInflater): TaskView

  /** Creates the view for body of the task. */
  abstract fun onCreateTaskBody(inflater: LayoutInflater): View

  /** Invoked after the task view gets attached to the fragment. */
  open fun onTaskViewAttached() {}

  /** Invoked when the task fragment is visible to the user. */
  open fun onTaskVisibleToUser() {}

  /** Invoked when the fragment is ready to add buttons to the current [TaskView]. */
  open fun onCreateActionButtons() {
    addSkipButton()
    addNextButton()
  }

  /** Invoked when the all [ButtonAction]s are added to the current [TaskView]. */
  open fun onActionButtonsCreated() {
    viewLifecycleOwner.lifecycleScope.launch { viewModel.taskValue.collect { onValueChanged(it) } }
  }

  /** Invoked when the data associated with the current task gets modified. */
  protected open fun onValueChanged(value: Value?) {
    for ((_, button) in buttons) {
      button.onValueChanged(value)
    }
  }

  private fun addPreviousButton() =
    addButton(ButtonAction.PREVIOUS)
      .setOnClickListener { moveToPrevious() }
      .showIfTrue(position != 0)

  protected fun addNextButton() =
    addButton(ButtonAction.NEXT)
      .setOnClickListener { moveToNext() }
      .setOnValueChanged { button, value -> button.enableIfTrue(value.isNotNullOrEmpty()) }
      .disable()

  /** Skip button is only visible iff the task is optional and the task doesn't contain any data. */
  protected fun addSkipButton() =
    addButton(ButtonAction.SKIP)
      .setOnClickListener { onSkip() }
      .setOnValueChanged { button, value ->
        button.showIfTrue(viewModel.isTaskOptional() && value.isNullOrEmpty())
      }
      .showIfTrue(viewModel.isTaskOptional())

  private fun onSkip() {
    check(viewModel.hasNoData()) { "User should not be able to skip a task with data." }
    moveToNext()
  }

  private fun moveToPrevious() {
    dataCollectionViewModel.onPreviousClicked(position, viewModel)
  }

  fun moveToNext() {
    lifecycleScope.launch { dataCollectionViewModel.onNextClicked(position, viewModel) }
  }

  fun addUndoButton() =
    addButton(ButtonAction.UNDO)
      .setOnClickListener { viewModel.clearResponse() }
      .setOnValueChanged { button, value -> button.showIfTrue(value.isNotNullOrEmpty()) }
      .hide()

  protected fun addButton(buttonAction: ButtonAction): TaskButton {
    val action = if (buttonAction.shouldReplaceWithDoneButton()) ButtonAction.DONE else buttonAction
    check(!buttons.contains(action)) { "Button $action already bound" }
    val button =
      TaskButtonFactory.createAndAttachButton(
        action,
        when (buttonAction.location) {
          ButtonAction.Location.START -> taskView.actionButtonsContainer.startButtons
          ButtonAction.Location.END -> taskView.actionButtonsContainer.endButtons
        },
        layoutInflater
      )
    buttonsIndex[buttons.size] = action
    buttons[action] = button
    return button
  }

  /** Returns true if the given [ButtonAction] should be replace with "Done" button. */
  private fun ButtonAction.shouldReplaceWithDoneButton() =
    this == ButtonAction.NEXT && dataCollectionViewModel.isLastPosition(position)

  @TestOnly fun getButtons() = buttons

  @TestOnly fun getButtonsIndex() = buttonsIndex

  companion object {
    /** Key used to store the position of the task in the Job's sorted tasklist. */
    const val POSITION = "position"
  }
}
