/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.datacollection.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import javax.inject.Provider
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment.Companion.TASK_ID_FRAGMENT_ARG_KEY

/**
 * A Composable that hosts a fragment-based map container for a specific task.
 *
 * This function bridges the gap between Jetpack Compose and the Fragment-based map implementation
 * by using [AndroidView] to embed a [FragmentContainerView].
 */
@Composable
fun TaskMapFragmentContainer(
  taskId: String,
  fragmentManager: FragmentManager,
  fragmentProvider: Provider<out AbstractMapContainerFragment>,
) {
  AndroidView(
    factory = { context -> FragmentContainerView(context).apply { id = View.generateViewId() } },
    update = { view ->
      with(fragmentProvider.get()) {
        arguments = bundleOf(Pair(TASK_ID_FRAGMENT_ARG_KEY, taskId))
        fragmentManager.beginTransaction().replace(view.id, this).commit()
      }
    },
  )
}
