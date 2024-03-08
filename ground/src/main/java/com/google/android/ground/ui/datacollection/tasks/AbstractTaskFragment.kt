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
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.model.task.Task
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
      .setOnClickListener { handleNext() }
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
    lifecycleScope.launch { dataCollectionViewModel.onPreviousClicked(viewModel) }
  }

  private fun moveToNext() {
    lifecycleScope.launch { dataCollectionViewModel.onNextClicked(viewModel) }
  }

  fun handleNext() {
    if (getTask().isAddLoiTask) {
      showLoiNameDialog(dataCollectionViewModel.loiName.value)
    } else {
      moveToNext()
    }
  }

  fun handleLoiNameSet(loiName: String) {
    if (loiName != "") {
      lifecycleScope.launch {
        dataCollectionViewModel.setLoiName(loiName)
        moveToNext()
      }
    }
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

  fun getTask(): Task = viewModel.task

  fun getCurrentValue(): Value? = viewModel.taskValue.value

  @TestOnly fun getButtons() = buttons

  @TestOnly fun getButtonsIndex() = buttonsIndex

  companion object {
    /** Key used to store the position of the task in the Job's sorted tasklist. */
    const val POSITION = "position"
  }

  private fun showLoiNameDialog(initialTextValue: String) {
    (view as ViewGroup).addView(
      ComposeView(requireContext()).apply {
        setContent {
          val openAlertDialog = remember { mutableStateOf(Pair(initialTextValue, true)) }
          when {
            openAlertDialog.value.second -> {
              val textFieldValue = openAlertDialog.value.first
              LoiNameDialog(
                textFieldValue = textFieldValue,
                onConfirmRequest = {
                  openAlertDialog.value = Pair(textFieldValue, false)
                  handleLoiNameSet(loiName = textFieldValue)
                },
                onDismissRequest = { openAlertDialog.value = Pair(initialTextValue, false) },
                onTextFieldChange = {
                  openAlertDialog.value = Pair(it, openAlertDialog.value.second)
                }
              )
            }
          }
        }
      }
    )
  }

  private fun getColor(id: Int): Color = Color(ContextCompat.getColor(requireContext(), id))

  @Composable
  private fun LoiNameDialog(
    textFieldValue: String,
    onConfirmRequest: () -> Unit,
    onDismissRequest: () -> Unit,
    onTextFieldChange: (String) -> Unit
  ) {
    val primaryColor = getColor(R.color.md_theme_primary)
    val onPrimaryColor = getColor(R.color.md_theme_onPrimary)
    val onSurfaceDisabledColor = getColor(R.color.md_theme_on_surface_disabled)
    val saveButtonColors =
      ButtonColors(
        containerColor = primaryColor,
        contentColor = onPrimaryColor,
        disabledContainerColor = onSurfaceDisabledColor,
        disabledContentColor = onPrimaryColor,
      )
    val cancelButtonColors =
      ButtonColors(
        containerColor = Color.Transparent,
        contentColor = primaryColor,
        disabledContainerColor = onSurfaceDisabledColor,
        disabledContentColor = onPrimaryColor,
      )
    val textFieldColors =
      TextFieldDefaults.colors(
        focusedIndicatorColor = primaryColor,
        unfocusedIndicatorColor = primaryColor,
        focusedContainerColor = getColor(R.color.md_theme_text_field_container),
        unfocusedContainerColor = getColor(R.color.md_theme_text_field_container),
        cursorColor = primaryColor,
      )
    AlertDialog(
      onDismissRequest = onDismissRequest,
      icon = {},
      title = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = getString(R.string.loi_name_dialog_title),
            fontSize = 5.em,
            textAlign = TextAlign.Start,
          )
        }
      },
      text = {
        Column {
          Text(
            text = getString(R.string.loi_name_dialog_body),
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp),
          )
          TextField(
            value = textFieldValue,
            onValueChange = onTextFieldChange,
            colors = textFieldColors,
            singleLine = true,
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = onConfirmRequest,
          colors = saveButtonColors,
          contentPadding = PaddingValues(25.dp, 0.dp),
          enabled = textFieldValue != "",
        ) {
          Text(getString(R.string.save))
        }
      },
      dismissButton = {
        TextButton(
          onClick = onDismissRequest,
          colors = cancelButtonColors,
          contentPadding = PaddingValues(20.dp, 0.dp),
          border = BorderStroke(2.dp, getColor(R.color.md_theme_outline)),
        ) {
          Text(getString(R.string.cancel))
        }
      },
      containerColor = getColor(R.color.md_theme_background),
      textContentColor = getColor(R.color.md_theme_onBackground),
    )
  }

  /** Supports annotated texts e.g. <b>Hello world</b> */
  @Composable
  private fun TitleText(text: CharSequence, modifier: Modifier = Modifier) {
    AndroidView(
      modifier = modifier,
      factory = { context -> TextView(context) },
      update = {
        it.text = text
        it.textSize = 24.toFloat()
      },
    )
  }
}
