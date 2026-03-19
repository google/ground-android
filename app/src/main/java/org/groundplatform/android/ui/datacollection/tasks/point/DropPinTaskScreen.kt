/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.point

import android.view.View
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment.Companion.TASK_ID_FRAGMENT_ARG_KEY
import org.groundplatform.android.ui.datacollection.tasks.TaskContainer
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment

@Composable
fun DropPinTaskScreen(viewModel: DropPinTaskViewModel, env: TaskScreenEnvironment) {
  val taskHeader = TaskHeader(viewModel.task.label, R.drawable.outline_pin_drop)
  val instructionData =
    InstructionData(iconId = R.drawable.swipe_24, stringId = R.string.drop_a_pin_tooltip_text)

  LaunchedEffect(Unit) {
    if (viewModel.shouldShowInstructionsDialog()) {
      viewModel.showInstructionsDialog.value = true
    }
  }

  TaskContainer(
    viewModel = viewModel,
    dataCollectionViewModel = env.dataCollectionViewModel,
    taskHeader = taskHeader,
    instructionData = instructionData,
    onInstructionDialogDismissed = { viewModel.instructionsDialogShown = true },
  ) {
    AndroidView(
      factory = { context ->
        LinearLayout(context).apply {
          id = View.generateViewId() * 11617
          val fragment = env.dropPinTaskMapFragmentProvider.get()
          fragment.arguments = bundleOf(Pair(TASK_ID_FRAGMENT_ARG_KEY, viewModel.task.id))
          env.fragmentManager.beginTransaction().add(id, fragment, "Drop a pin fragment").commit()
        }
      }
    )
  }
}
