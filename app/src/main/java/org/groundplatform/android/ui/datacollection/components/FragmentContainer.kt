package org.groundplatform.android.ui.datacollection.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentContainerView
import javax.inject.Provider
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment.Companion.TASK_ID_FRAGMENT_ARG_KEY
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment

@Composable
fun FragmentContainer(
  env: TaskScreenEnvironment,
  taskId: String,
  fragmentProvider: Provider<out AbstractMapContainerFragment>,
) {
  AndroidView(
    factory = { context -> FragmentContainerView(context).apply { id = View.generateViewId() } },
    update = { view ->
      with(fragmentProvider.get()) {
        arguments = bundleOf(Pair(TASK_ID_FRAGMENT_ARG_KEY, taskId))
        env.fragmentManager.beginTransaction().replace(view.id, this).commit()
      }
    },
  )
}
