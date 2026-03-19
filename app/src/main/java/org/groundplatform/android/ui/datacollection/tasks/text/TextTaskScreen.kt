/*
 * Copyright 2022 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.groundplatform.android.model.submission.TextTaskData.Companion.fromString
import org.groundplatform.android.ui.datacollection.components.TextTaskInput
import org.groundplatform.android.ui.datacollection.tasks.TaskContainer
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.groundplatform.ui.theme.sizes

const val INPUT_TEXT_TEST_TAG: String = "text task input test tag"

@Composable
fun TextTaskScreen(viewModel: TextTaskViewModel, env: TaskScreenEnvironment) {
  val userResponse by viewModel.responseText.observeAsState("")

  TaskContainer(viewModel = viewModel, dataCollectionViewModel = env.dataCollectionViewModel) {
    TextTaskInput(
      userResponse,
      modifier =
        Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding)
          .testTag(INPUT_TEXT_TEST_TAG),
    ) { newText ->
      viewModel.setValue(fromString(newText))
    }
  }
}
