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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.core.view.doOnAttach
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.properties.Delegates
import org.groundplatform.android.R
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.datacollection.DataCollectionUiState
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.Header
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskFooter
import org.groundplatform.android.ui.datacollection.components.TaskViewLayout
import org.groundplatform.android.util.createComposeView

abstract class AbstractTaskFragment<T : AbstractTaskViewModel> : AbstractFragment() {

  protected val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)

  /** ID of the associated task in the Job. Used for instantiating the [viewModel]. */
  var taskId by Delegates.notNull<String>()

  protected val viewModel: T by lazy {
    @Suppress("UNCHECKED_CAST")
    dataCollectionViewModel.getTaskViewModel(taskId) as? T
      ?: error("ViewModel for taskId:$taskId not found.")
  }

  /** Represents the content to be shown in the task header, if any. */
  open val taskHeader: Header? by lazy {
    Header(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer)
  }

  /** Represents the content to be shown in the task instructions, if any. */
  open val instructionData: InstructionData? = null

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
  ): View = createComposeView {
    TaskViewLayout(header = taskHeader, footer = { TaskFooter() }, content = { TaskBody() })

    if (getTask().isAddLoiTask) {
      LoiNameDialog()
    }

    instructionData?.let { InstructionsDialog(it) }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.doOnAttach { onTaskViewAttached() }
  }

  override fun onResume() {
    super.onResume()
    onTaskResume()
  }

  /** Renders the body of the task. */
  @Composable abstract fun TaskBody()

  /** Invoked when the instruction dialog is dismissed. */
  open fun onInstructionDialogDismissed() {}

  /** Invoked after the task view gets attached to the fragment. */
  open fun onTaskViewAttached() {}

  /** Invoked when the task fragment is visible to the user. */
  open fun onTaskResume() {}

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
      dataCollectionViewModel.loiNameDialogOpen.value = true
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

  /** Adds the action buttons to the UI. */
  @Composable
  internal fun TaskFooter() {
    val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
    TaskFooter(
      modifier = Modifier.onGloballyPositioned { saveFooterPosition(it.positionInWindow().y) },
      headerCard =
        if (shouldShowHeader()) {
          { HeaderCard() }
        } else {
          null
        },
      buttonActionStates = taskActionButtonsStates,
      onButtonClicked = { handleButtonClick(it) },
    )
  }

  private fun saveFooterPosition(top: Float) {
    dataCollectionViewModel.updateFooterPosition(top)
  }

  private fun handleButtonClick(action: ButtonAction) {
    when (action) {
      // Navigation actions
      ButtonAction.PREVIOUS -> moveToPrevious()
      ButtonAction.NEXT,
      ButtonAction.DONE -> handleNext()
      ButtonAction.SKIP -> onSkip()
      // Task-specific actions - delegate to ViewModel
      else -> viewModel.onButtonClick(action)
    }
  }

  // This function can allow any task to show a Header card on top of the Button row.
  open fun shouldShowHeader() = false

  @Composable open fun HeaderCard() {}

  private fun getTask(): Task = viewModel.task

  @Composable
  private fun LoiNameDialog() {
    var openAlertDialog by dataCollectionViewModel.loiNameDialogOpen

    if (openAlertDialog) {
      val uiState by dataCollectionViewModel.uiState.collectAsStateWithLifecycle()
      val initialNameValue =
        (uiState as? DataCollectionUiState.Ready)?.loiName
          ?: dataCollectionViewModel.getTypedLoiNameOrEmpty()
      var name by remember(initialNameValue) { mutableStateOf(initialNameValue) }

      LoiNameDialog(
        textFieldValue = name,
        onConfirmRequest = {
          openAlertDialog = false
          handleLoiNameSet(name)
        },
        onDismissRequest = {
          name = initialNameValue
          openAlertDialog = false
        },
        onTextFieldChange = { name = it },
      )
    }
  }

  @Composable
  private fun InstructionsDialog(instructionData: InstructionData) {
    var showInstructionsDialog by viewModel.showInstructionsDialog

    if (showInstructionsDialog) {
      InstructionsDialog(iconId = instructionData.iconId, stringId = instructionData.stringId) {
        showInstructionsDialog = false
        onInstructionDialogDismissed()
      }
    }
  }

  companion object {
    const val TASK_ID = "taskId"
  }
}
