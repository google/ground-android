package org.groundplatform.android.ui.datacollection.tasks

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.LoiNameDialog
import org.groundplatform.android.ui.datacollection.components.TaskFooter
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TaskViewLayout
import org.groundplatform.domain.model.task.Task

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(
  task: Task,
  taskHeader: TaskHeader?,
  instructionData: InstructionData?,
  taskActionButtonsStates: List<ButtonActionState>,
  loiNameDialogOpen: Boolean,
  shouldShowHeader: Boolean,
  showInstructionsDialog: Boolean,
  initialNameValue: String,
  onFooterPositionUpdated: (Float) -> Unit,
  onButtonClicked: (ButtonAction) -> Unit,
  onLoiNameConfirm: (String) -> Unit,
  onLoiNameDismiss: () -> Unit,
  onInstructionsDismiss: () -> Unit,
  headerCard: @Composable (() -> Unit)?,
  taskBody: @Composable () -> Unit,
) {
  val isKeyboardOpen = WindowInsets.isImeVisible
  var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

  // Update footer position whenever layout changes or keyboard is toggled.
  LaunchedEffect(isKeyboardOpen, layoutCoordinates) {
    layoutCoordinates?.let { onFooterPositionUpdated(it.positionInWindow().y) }
  }

  TaskViewLayout(
    header = taskHeader,
    footer = {
      TaskFooter(
        modifier = Modifier.onGloballyPositioned { layoutCoordinates = it },
        headerCard = headerCard.takeIf { shouldShowHeader },
        buttonActionStates = taskActionButtonsStates,
        onButtonClicked = onButtonClicked,
      )
    },
    content = { taskBody() },
  )

  if (task.isAddLoiTask && loiNameDialogOpen) {
    val nameState = rememberSaveable { mutableStateOf(initialNameValue) }

    LoiNameDialog(
      textFieldValue = nameState.value,
      onConfirmRequest = { onLoiNameConfirm(nameState.value) },
      onDismissRequest = {
        nameState.value = initialNameValue
        onLoiNameDismiss()
      },
      onTextFieldChange = { nameState.value = it },
    )
  }

  instructionData
    ?.takeIf { showInstructionsDialog }
    ?.let { InstructionsDialog(data = it, onDismissed = onInstructionsDismiss) }
}
