package org.groundplatform.android.ui.datacollection

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import org.groundplatform.android.model.task.Task

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskPager(
  tasks: List<Task>,
  taskPosition: TaskPosition,
  fragmentManager: FragmentManager,
  taskFragmentProvider: TaskFragmentProvider,
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
    val viewId = rememberSaveable { View.generateViewId() }

    DisposableEffect(task.id) {
      onDispose {
        val fragment = fragmentManager.findFragmentByTag(task.id)
        if (fragment != null) {
          fragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
        }
      }
    }

    AndroidView(
      factory = { context -> FragmentContainerView(context).apply { id = viewId } },
      update = { view ->
        val existing = fragmentManager.findFragmentByTag(task.id)
        if (existing == null) {
          val fragment = taskFragmentProvider.getFragmentForTask(task)
          fragmentManager
            .beginTransaction()
            .replace(view.id, fragment, task.id)
            .commitAllowingStateLoss()
        }
      },
    )
  }
}
