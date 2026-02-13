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
package org.groundplatform.android.ui.datacollection.tasks

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel

/**
 * Launches a coroutine that runs the given [block] only when the task with [taskId] is visible to
 * the user (active in ViewPager) and the fragment is in at least the STARTED state. The block is
 * automatically canceled when the task becomes inactive.
 */
internal fun Fragment.launchWhenTaskVisible(
  viewModel: DataCollectionViewModel,
  taskId: String,
  block: suspend CoroutineScope.() -> Unit,
) {
  viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.currentActiveTaskFlow(taskId).collectLatest { isActive ->
        if (isActive) {
          coroutineScope { block() }
        }
      }
    }
  }
}
