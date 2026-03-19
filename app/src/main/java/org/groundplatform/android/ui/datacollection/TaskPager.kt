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
  captureLocationTaskMapFragmentProvider: Provider<CaptureLocationTaskMapFragment>,
  dataCollectionViewModel: DataCollectionViewModel,
  drawAreaTaskMapFragmentProvider: Provider<DrawAreaTaskMapFragment>,
  dropPinTaskMapFragmentProvider: Provider<DropPinTaskMapFragment>,
  fragmentManager: FragmentManager,
  homeScreenViewModel: HomeScreenViewModel,
  permissionsManager: PermissionsManager,
  popups: EphemeralPopups,
  taskPosition: TaskPosition,
  tasks: List<Task>,
) {
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
    val taskViewModel = dataCollectionViewModel.getTaskViewModel(task.id)

    if (taskViewModel != null) {
      when (taskViewModel) {
        is CaptureLocationTaskViewModel ->
          CaptureLocationTaskScreen(
            taskViewModel,
            dataCollectionViewModel,
            captureLocationTaskMapFragmentProvider,
            fragmentManager,
          )
        is DateTaskViewModel -> DateTaskScreen(taskViewModel, dataCollectionViewModel)
        is DrawAreaTaskViewModel ->
          DrawAreaTaskScreen(
            taskViewModel,
            dataCollectionViewModel,
            drawAreaTaskMapFragmentProvider,
            fragmentManager,
          )
        is DropPinTaskViewModel ->
          DropPinTaskScreen(
            taskViewModel,
            dataCollectionViewModel,
            dropPinTaskMapFragmentProvider,
            fragmentManager,
          )
        is InstructionTaskViewModel -> InstructionTaskScreen(taskViewModel, dataCollectionViewModel)
        is MultipleChoiceTaskViewModel ->
          MultipleChoiceTaskScreen(taskViewModel, dataCollectionViewModel)
        is NumberTaskViewModel -> NumberTaskScreen(taskViewModel, dataCollectionViewModel)
        is PhotoTaskViewModel ->
          PhotoTaskScreen(
            taskViewModel,
            dataCollectionViewModel,
            homeScreenViewModel,
            permissionsManager,
            popups,
          )
        is TextTaskViewModel -> TextTaskScreen(taskViewModel, dataCollectionViewModel)
        is TimeTaskViewModel -> TimeTaskScreen(taskViewModel, dataCollectionViewModel)
        else -> error("Unhandled task ViewModel type: ${taskViewModel.javaClass.name}")
      }
    }
  }
}
