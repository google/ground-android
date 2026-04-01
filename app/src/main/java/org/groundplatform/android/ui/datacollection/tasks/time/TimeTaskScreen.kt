package org.groundplatform.android.ui.datacollection.tasks.time

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.submission.DateTimeTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.ui.theme.sizes

const val TIME_PICKER_TEST_TAG: String = "time picker test tag"

@Composable
fun TimeTaskScreen(
  viewModel: TimeTaskViewModel,
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val taskData by viewModel.taskTaskData.collectAsStateWithLifecycle()

  TaskScreen(
    taskHeader =
      TaskHeader(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer),
    taskActionButtonsStates = taskActionButtonsStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    taskBody = {
      TimeTaskContent(
        taskData,
        onTimeSelected = { viewModel.updateResponse(it) },
        onResponseCleared = { viewModel.clearResponse() },
      )
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeTaskContent(
  taskData: TaskData?,
  onTimeSelected: (Long) -> Unit,
  onResponseCleared: () -> Unit,
) {
  val context = LocalContext.current
  var showDialog by rememberSaveable { mutableStateOf(false) }

  val timeText =
    remember(taskData) {
      (taskData as? DateTimeTaskData)?.let {
        DateFormat.getTimeFormat(context).format(Date(it.timeInMillis))
      } ?: ""
    }

  val hintText = remember {
    (DateFormat.getTimeFormat(context) as? SimpleDateFormat)?.toPattern()?.uppercase()
      ?: "HH:MM AM/PM"
  }

  TimeTaskField(
    modifier = Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding),
    timeText = timeText,
    hintText = hintText,
    onTimeClick = { showDialog = true },
  )

  if (showDialog) {
    TimeSelectionDialog(
      initialTime = (taskData as? DateTimeTaskData)?.timeInMillis ?: System.currentTimeMillis(),
      onTimeSelected = onTimeSelected,
      onClear = {
        onResponseCleared()
        showDialog = false
      },
      onDismiss = { showDialog = false },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectionDialog(
  initialTime: Long?,
  onTimeSelected: (Long) -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
) {
  val calendar = Calendar.getInstance()
  if (initialTime != null) {
    calendar.timeInMillis = initialTime
  }

  val timePickerState =
    rememberTimePickerState(
      initialHour = calendar.get(Calendar.HOUR_OF_DAY),
      initialMinute = calendar.get(Calendar.MINUTE),
      is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )

  TimePickerDialog(
    modifier = Modifier.testTag(TIME_PICKER_TEST_TAG),
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          val c = Calendar.getInstance()
          c.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
          c.set(Calendar.MINUTE, timePickerState.minute)
          onTimeSelected(c.time.time)
          onDismiss()
        }
      ) {
        Text(stringResource(android.R.string.ok))
      }
    },
    dismissButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) } },
    title = {},
  ) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
      TimePicker(state = timePickerState)
    }
  }
}
