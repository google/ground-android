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
package org.groundplatform.android.ui.datacollection.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.doOnAttach
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import kotlin.properties.Delegates
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.isNotNullOrEmpty
import org.groundplatform.android.model.submission.isNullOrEmpty
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskButton
import org.groundplatform.android.ui.datacollection.components.TaskView
import org.groundplatform.android.util.renderComposableDialog
import org.groundplatform.android.util.setComposableContent
import org.jetbrains.annotations.TestOnly
import timber.log.Timber

abstract class AbstractTaskFragment<T : AbstractTaskViewModel> : AbstractFragment() {

  protected val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)

  @TestOnly val buttonDataList: MutableList<ButtonData> = mutableListOf()

  private lateinit var taskView: TaskView
  protected lateinit var viewModel: T

  /** ID of the associated task in the Job. Used for instantiating the [viewModel]. */
  var taskId by Delegates.notNull<String>()

  protected val isViewModelInitialized: Boolean
    get() =
      try {
        viewModel
        true
      } catch (e: UninitializedPropertyAccessException) {
        Timber.d("Viewmodel is not initialized", e)
        false
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      taskId = requireNotNull(savedInstanceState.getString(TASK_ID))
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(TASK_ID, taskId)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    taskView = onCreateTaskView(inflater)
    return taskView.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.doOnAttach {
      @Suppress("UNCHECKED_CAST", "LabeledExpression")
      val vm = dataCollectionViewModel.getTaskViewModel(taskId) as? T ?: return@doOnAttach

      viewModel = vm
      taskView.bind(this, viewModel)
      taskView.addTaskView(onCreateTaskBody(layoutInflater))

      // Add actions buttons after the view model is bound to the view.
      addPreviousButton()
      onCreateActionButtons()
      renderButtons()
      onActionButtonsCreated()

      onTaskViewAttached()
    }
  }

  override fun onResume() {
    super.onResume()
    onTaskResume()
  }

  /** Creates the view for common task template with/without header. */
  abstract fun onCreateTaskView(inflater: LayoutInflater): TaskView

  /** Creates the view for body of the task. */
  abstract fun onCreateTaskBody(inflater: LayoutInflater): View

  /** Invoked after the task view gets attached to the fragment. */
  open fun onTaskViewAttached() {}

  /** Invoked when the task fragment is visible to the user. */
  open fun onTaskResume() {}

  /** Invoked when the fragment is ready to add buttons to the current [TaskView]. */
  open fun onCreateActionButtons() {
    addSkipButton()
    addNextButton()
  }

  /** Invoked when the all [ButtonAction]s are added to the current [TaskView]. */
  open fun onActionButtonsCreated() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.taskTaskData.collect { onValueChanged(it) }
    }
  }

  /** Invoked when the data associated with the current task gets modified. */
  protected open fun onValueChanged(taskData: TaskData?) {
    for ((_, button) in buttonDataList) {
      button.onValueChanged(taskData)
    }
  }

  private fun addPreviousButton() =
    addButton(ButtonAction.PREVIOUS)
      .setOnClickListener { moveToPrevious() }
      .enableIfTrue(!dataCollectionViewModel.isFirstPosition(taskId))

  protected fun addNextButton(hideIfEmpty: Boolean = false) =
    addButton(ButtonAction.NEXT)
      .setOnClickListener { handleNext() }
      .setOnValueChanged { button, value ->
        if (hideIfEmpty) {
          button.showIfTrue(value.isNotNullOrEmpty())
        }
        button.enableIfTrue(value.isNotNullOrEmpty())
        val isLastPosition = dataCollectionViewModel.isLastPositionWithValue(viewModel.task, value)
        button.toggleDone(done = isLastPosition)
      }
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
    viewModel.setSkipped()
    moveToNext()
  }

  private fun moveToPrevious() {
    dataCollectionViewModel.onPreviousClicked(viewModel)
  }

  fun moveToNext() {
    dataCollectionViewModel.onNextClicked(viewModel)
  }

  private fun handleNext() {
    if (getTask().isAddLoiTask) {
      launchLoiNameDialog()
    } else {
      moveToNext()
    }
  }

  private fun handleLoiNameSet(loiName: String) {
    if (loiName != "") {
      dataCollectionViewModel.setLoiName(loiName)
      moveToNext()
    }
  }

  fun addUndoButton() = addUndoButton { viewModel.clearResponse() }

  fun addUndoButton(clickHandler: () -> Unit) =
    addButton(ButtonAction.UNDO)
      .setOnClickListener { clickHandler() }
      .setOnValueChanged { button, value -> button.showIfTrue(value.isNotNullOrEmpty()) }
      .hide()

  protected fun addButton(buttonAction: ButtonAction): TaskButton {
    val action =
      if (buttonAction == ButtonAction.NEXT && isLastPosition()) ButtonAction.DONE else buttonAction
    check(!buttonDataList.any { it.button.getAction() == action }) {
      "Button $action already bound"
    }
    val button = TaskButton(action)
    buttonDataList.add(ButtonData(index = buttonDataList.size, button))
    return button
  }

  /** Adds the action buttons to the UI. */
  private fun renderButtons() {
    taskView.actionButtonsContainer.composeView.setComposableContent {
      Row(horizontalArrangement = Arrangement.SpaceBetween) {
        buttonDataList.sortedBy { it.index }.forEach { (_, button) -> button.CreateButton() }
      }
    }
  }

  /** Returns true if the current task is in the last position in the sequence. */
  private fun isLastPosition() = dataCollectionViewModel.isLastPosition(taskId)

  private fun getTask(): Task = viewModel.task

  fun getCurrentValue(): TaskData? = viewModel.taskTaskData.value

  private fun launchLoiNameDialog() {
    dataCollectionViewModel.loiNameDialogOpen.value = true
    renderComposableDialog {
      // The LOI NameDialog should call `handleLoiNameSet()` to continue to the next task.
      ShowLoiNameDialog(dataCollectionViewModel.loiName.value ?: "") {
        handleLoiNameSet(loiName = it)
      }
    }
  }

  @Composable
  fun ShowLoiNameDialog(initialNameValue: String, onNameSet: (String) -> Unit) {
    var openAlertDialog by rememberSaveable { dataCollectionViewModel.loiNameDialogOpen }
    var name by rememberSaveable { mutableStateOf(initialNameValue) }
    if (openAlertDialog) {
      LoiNameDialog(
        textFieldValue = name,
        onConfirmRequest = {
          openAlertDialog = false
          onNameSet(name)
        },
        onDismissRequest = {
          name = initialNameValue
          openAlertDialog = false
        },
        onTextFieldChange = { name = it },
      )
    }
  }

  data class ButtonData(val index: Int, val button: TaskButton)

  companion object {
    const val TASK_ID = "taskId"
  }
}
