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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.ui.datacollection.components.TaskMapFragmentContainer
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.util.createComposeView
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class DropPinTaskFragment @Inject constructor() : AbstractTaskFragment<DropPinTaskViewModel>() {
  @Inject lateinit var dropPinTaskMapFragmentProvider: Provider<DropPinTaskMapFragment>

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ) = createComposeView {
    val shouldShowLoiNameDialog by dataCollectionViewModel.loiNameDialogOpen
    val loiName by dataCollectionViewModel.loiNameDraft.collectAsStateWithLifecycle()

    DropPinTaskScreen(
      viewModel = viewModel,
      onFooterPositionUpdated = { saveFooterPosition(it) },
      shouldShowLoiNameDialog = shouldShowLoiNameDialog,
      loiName = loiName,
      onAction = { action -> handleTaskScreenAction(action) },
    ) {
      TaskMapFragmentContainer(
        taskId = viewModel.task.id,
        fragmentManager = childFragmentManager,
        fragmentProvider = dropPinTaskMapFragmentProvider,
      )
    }
  }
}
