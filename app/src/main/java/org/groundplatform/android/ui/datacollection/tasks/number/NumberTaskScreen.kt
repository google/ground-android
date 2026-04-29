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
package org.groundplatform.android.ui.datacollection.tasks.number

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.components.TextTaskInput
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.domain.model.submission.NumberTaskData.Companion.fromNumber
import org.groundplatform.ui.theme.AppTheme
import org.groundplatform.ui.theme.sizes

const val INPUT_NUMBER_TEST_TAG: String = "number task input test tag"

@Composable
fun NumberTaskScreen(
  viewModel: NumberTaskViewModel,
  onFooterPositionUpdated: (Float) -> Unit,
  onButtonClicked: (ButtonAction) -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val userResponse by viewModel.responseText.observeAsState("")

  TaskScreen(
    taskHeader =
      TaskHeader(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer),
    taskActionButtonsStates = taskActionButtonsStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onButtonClicked = onButtonClicked,
    taskBody = {
      NumberTaskContent(
        userResponse = userResponse,
        onValueChanged = { viewModel.setValue(fromNumber(it)) },
      )
    },
  )
}

@Composable
internal fun NumberTaskContent(userResponse: String, onValueChanged: (String) -> Unit) {
  TextTaskInput(
    userResponse,
    keyboardType = KeyboardType.Decimal,
    modifier =
      Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding)
        .testTag(INPUT_NUMBER_TEST_TAG),
  ) { newText ->
    onValueChanged(newText)
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun NumberTaskContentPreviewEmpty() {
  AppTheme { NumberTaskContent(userResponse = "", onValueChanged = {}) }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun NumberTaskContentPreviewWithNumber() {
  AppTheme { NumberTaskContent(userResponse = "123.45", onValueChanged = {}) }
}
