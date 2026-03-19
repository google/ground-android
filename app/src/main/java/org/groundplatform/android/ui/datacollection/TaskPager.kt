package org.groundplatform.android.ui.datacollection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskPager(env: TaskScreenEnvironment, taskPosition: TaskPosition, tasks: List<Task>) {
  val pagerState =
    rememberPagerState(initialPage = taskPosition.absoluteIndex, pageCount = { tasks.size })

  LaunchedEffect(taskPosition.absoluteIndex) {
    if (pagerState.currentPage != taskPosition.absoluteIndex) {
      pagerState.animateScrollToPage(taskPosition.absoluteIndex)
    }
  }

  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    userScrollEnabled = false,
  ) { page ->
    val task = tasks[page]
    val taskViewModel = env.dataCollectionViewModel.getTaskViewModel(task.id)

    if (taskViewModel != null) {
      when (taskViewModel) {
        is CaptureLocationTaskViewModel -> CaptureLocationTaskScreen(taskViewModel, env)
        is DateTaskViewModel -> DateTaskScreen(taskViewModel, env)
        is DrawAreaTaskViewModel -> DrawAreaTaskScreen(taskViewModel, env)
        is DropPinTaskViewModel -> DropPinTaskScreen(taskViewModel, env)
        is InstructionTaskViewModel -> InstructionTaskScreen(taskViewModel, env)
        is MultipleChoiceTaskViewModel -> MultipleChoiceTaskScreen(taskViewModel, env)
        is NumberTaskViewModel -> NumberTaskScreen(taskViewModel, env)
        is PhotoTaskViewModel -> PhotoTaskScreen(taskViewModel, env)
        is TextTaskViewModel -> TextTaskScreen(taskViewModel, env)
        is TimeTaskViewModel -> TimeTaskScreen(taskViewModel, env)
        else -> error("Unhandled task ViewModel type: ${taskViewModel.javaClass.name}")
      }
    }
  }
}
