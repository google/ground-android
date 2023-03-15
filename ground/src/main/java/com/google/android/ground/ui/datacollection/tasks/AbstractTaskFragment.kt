package com.google.android.ground.ui.datacollection.tasks

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.activityViewModels
import com.google.android.ground.R
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskButton
import com.google.android.ground.ui.datacollection.components.TaskView
import java.util.*
import kotlin.properties.Delegates

abstract class AbstractTaskFragment<T : AbstractTaskViewModel> : AbstractFragment() {

  private val actions: EnumMap<ButtonAction, () -> Unit> = EnumMap(ButtonAction::class.java)
  private val buttons: EnumMap<ButtonAction, TaskButton> = EnumMap(ButtonAction::class.java)

  protected val dataCollectionViewModel: DataCollectionViewModel by activityViewModels()
  protected lateinit var taskView: TaskView

  lateinit var viewModel: T

  /** Position of the task in the Job's sorted tasklist. Used to instantiate the ViewModel. */
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

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    onCreateActionButtons()
    initialState()

    viewModel.taskData.observe(viewLifecycleOwner) { refreshState(it.orElse(null)) }
  }

  open fun onCreateActionButtons() {
    addContinueButton()
    addSkipButton()
  }

  fun addContinueButton() {
    createButton(R.string.continue_text, null, ButtonAction.CONTINUE) {
      dataCollectionViewModel.onContinueClicked()
    }
  }

  fun addSkipButton() {
    if (viewModel.isTaskOptional()) {
      createButton(R.string.skip, null, ButtonAction.SKIP) {
        viewModel.clearResponse()
        dataCollectionViewModel.onContinueClicked()
      }
    }
  }

  fun addUndoButton() {
    createButton(null, R.drawable.ic_undo_black, ButtonAction.UNDO) { viewModel.clearResponse() }
  }

  protected open fun initialState() {
    maybeGetButton(ButtonAction.SKIP)?.apply { isEnabled = viewModel.isTaskOptional() }
    maybeGetButton(ButtonAction.UNDO)?.apply {
      visibility = View.GONE
      isEnabled = true
    }
  }

  protected open fun refreshState(taskData: TaskData?) {
    val isTaskEmpty = taskData?.isEmpty() ?: true
    getButton(ButtonAction.CONTINUE).apply { isEnabled = !isTaskEmpty }
  }

  fun createButton(
    @StringRes textId: Int?,
    @DrawableRes drawableId: Int?,
    action: ButtonAction,
    clickHandler: () -> Unit
  ) {
    val container = taskView.bottomControls
    val taskButton =
      TaskButton.createAndAttachButton(action, container, layoutInflater, drawableId, textId)
    val button = taskButton.view
    button.setOnClickListener { invokeAction(action) }
    bindAction(action, clickHandler, taskButton)
  }

  private fun bindAction(action: ButtonAction, clickHandler: () -> Unit, button: TaskButton) {
    check(!actions.contains(action)) { "Action $action already bound" }
    check(!buttons.contains(action)) { "Button $action already bound" }
    actions[action] = clickHandler
    buttons[action] = button
  }

  private fun invokeAction(action: ButtonAction) {
    check(actions.contains(action)) { "Expected key $action in $actions" }
    actions[action]?.invoke()
  }

  private fun maybeGetButton(action: ButtonAction): View? {
    return buttons[action]?.view
  }

  protected fun getButton(action: ButtonAction): View {
    check(buttons.contains(action)) { "Expected key $action in $buttons" }
    return buttons[action]!!.view
  }

  companion object {
    /** Key used to store the position of the task in the Job's sorted tasklist. */
    private const val POSITION = "position"
  }
}
