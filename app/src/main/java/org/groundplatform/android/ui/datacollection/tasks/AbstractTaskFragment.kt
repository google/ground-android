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
package org.groundplatform.android.ui.datacollection.tasks

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel

abstract class AbstractTaskFragment<T : AbstractTaskViewModel> : AbstractFragment() {

  protected val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)

  protected val taskId: String by lazy {
    arguments?.getString(TASK_ID) ?: error("taskId not found in arguments")
  }

  protected val viewModel: T by lazy {
    @Suppress("UNCHECKED_CAST")
    dataCollectionViewModel.getTaskViewModel(taskId) as? T
      ?: error("ViewModel for taskId:$taskId not found.")
  }

  companion object {
    const val TASK_ID = "taskId"
  }
}
