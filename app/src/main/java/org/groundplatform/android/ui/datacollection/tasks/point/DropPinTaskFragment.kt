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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import org.groundplatform.android.ui.datacollection.components.TaskMapFragmentContainer
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.util.createComposeView

@AndroidEntryPoint
class DropPinTaskFragment @Inject constructor() : AbstractTaskFragment<DropPinTaskViewModel>() {
  @Inject lateinit var dropPinTaskMapFragmentProvider: Provider<DropPinTaskMapFragment>

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ) = createComposeView {
    DropPinTaskScreen(
      viewModel = viewModel,
      onFooterPositionUpdated = { saveFooterPosition(it) },
      onAction = { handleTaskScreenAction(it) },
    ) {
      TaskMapFragmentContainer(
        taskId = viewModel.task.id,
        fragmentManager = childFragmentManager,
        fragmentProvider = dropPinTaskMapFragmentProvider,
      )
    }

    if (viewModel.task.isAddLoiTask) {
      LoiNameDialog()
    }
  }
}
