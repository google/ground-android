package org.groundplatform.android.ui.datacollection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentManager
import javax.inject.Provider
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskMapFragment
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskMapFragment
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskMapFragment
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskScreen
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskViewModel
import org.groundplatform.android.ui.home.HomeScreenViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskPager(
  tasks: List<Task>,
  taskPosition: TaskPosition,
  dataCollectionViewModel: DataCollectionViewModel,
  homeScreenViewModel: HomeScreenViewModel,
  permissionsManager: PermissionsManager,
  popups: EphemeralPopups,
  fragmentManager: FragmentManager,
  captureLocationTaskMapFragmentProvider: Provider<CaptureLocationTaskMapFragment>,
  drawAreaTaskMapFragmentProvider: Provider<DrawAreaTaskMapFragment>,
  dropPinTaskMapFragmentProvider: Provider<DropPinTaskMapFragment>,
) {
  val pagerState =
    rememberPagerState(initialPage = taskPosition.absoluteIndex, pageCount = { tasks.size })

  LaunchedEffect(taskPosition.absoluteIndex) {
    if (pagerState.currentPage != taskPosition.absoluteIndex) {
      pagerState.scrollToPage(taskPosition.absoluteIndex)
    }
  }

  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    userScrollEnabled = false,
  ) { page ->
    val task = tasks[page]
    val taskViewModel = dataCollectionViewModel.getTaskViewModel(task.id)

    if (taskViewModel != null) {
      when (task.type) {
        Task.Type.TEXT ->
          TextTaskScreen(taskViewModel as TextTaskViewModel, dataCollectionViewModel)
        Task.Type.MULTIPLE_CHOICE ->
          MultipleChoiceTaskScreen(
            taskViewModel as MultipleChoiceTaskViewModel,
            dataCollectionViewModel,
          )
        Task.Type.PHOTO ->
          PhotoTaskScreen(
            taskViewModel as PhotoTaskViewModel,
            dataCollectionViewModel,
            homeScreenViewModel,
            permissionsManager,
            popups,
          )
        Task.Type.DROP_PIN ->
          DropPinTaskScreen(
            taskViewModel as DropPinTaskViewModel,
            dataCollectionViewModel,
            dropPinTaskMapFragmentProvider,
            fragmentManager,
          )
        Task.Type.DRAW_AREA ->
          DrawAreaTaskScreen(
            taskViewModel as DrawAreaTaskViewModel,
            dataCollectionViewModel,
            drawAreaTaskMapFragmentProvider,
            fragmentManager,
          )
        Task.Type.NUMBER ->
          NumberTaskScreen(taskViewModel as NumberTaskViewModel, dataCollectionViewModel)
        Task.Type.DATE ->
          DateTaskScreen(taskViewModel as DateTaskViewModel, dataCollectionViewModel)
        Task.Type.TIME ->
          TimeTaskScreen(taskViewModel as TimeTaskViewModel, dataCollectionViewModel)
        Task.Type.CAPTURE_LOCATION ->
          CaptureLocationTaskScreen(
            taskViewModel as CaptureLocationTaskViewModel,
            dataCollectionViewModel,
            captureLocationTaskMapFragmentProvider,
            fragmentManager,
          )
        Task.Type.INSTRUCTIONS ->
          InstructionTaskScreen(taskViewModel as InstructionTaskViewModel, dataCollectionViewModel)
        Task.Type.UNKNOWN -> error("Unhandled task type: ${task.type}")
      }
    }
  }
}
