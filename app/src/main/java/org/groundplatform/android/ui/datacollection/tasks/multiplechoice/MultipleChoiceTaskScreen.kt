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
package org.groundplatform.android.ui.datacollection.tasks.multiplechoice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.groundplatform.android.ui.datacollection.tasks.TaskScreen
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.ui.theme.sizes

@Composable
fun MultipleChoiceTaskScreen(
  viewModel: MultipleChoiceTaskViewModel,
  onFooterPositionUpdated: (Float) -> Unit,
  onAction: (TaskScreenAction) -> Unit,
) {
  val taskActionButtonsStates by viewModel.taskActionButtonStates.collectAsStateWithLifecycle()
  val list by viewModel.items.collectAsStateWithLifecycle()

  TaskScreen(
    taskHeader =
      TaskHeader(label = viewModel.task.label, iconResId = R.drawable.ic_question_answer),
    taskActionButtonsStates = taskActionButtonsStates,
    onFooterPositionUpdated = onFooterPositionUpdated,
    onAction = onAction,
    taskBody = {
      MultipleChoiceTaskContent(
        list = list,
        onItemToggled = { viewModel.onItemToggled(it) },
        onOtherValueChanged = { viewModel.onOtherTextChanged(it) },
      )
    },
  )
}

@Composable
internal fun MultipleChoiceTaskContent(
  list: List<MultipleChoiceItem>,
  onItemToggled: (MultipleChoiceItem) -> Unit,
  onOtherValueChanged: (String) -> Unit,
) {
  val scrollState = rememberLazyListState()

  Box(modifier = Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding)) {
    LazyColumn(Modifier.testTag(MULTIPLE_CHOICE_LIST_TEST_TAG), state = scrollState) {
      items(list, key = { it.option.id }) { item ->
        MultipleChoiceItemView(
          item = item,
          isLastIndex = list.indexOf(item) == list.lastIndex,
          toggleItem = onItemToggled,
          otherValueChanged = onOtherValueChanged,
        )
      }
    }
  }
}
