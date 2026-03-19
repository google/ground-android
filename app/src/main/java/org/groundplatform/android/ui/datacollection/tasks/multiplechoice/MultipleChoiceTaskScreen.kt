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
import org.groundplatform.android.ui.datacollection.tasks.TaskContainer
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.groundplatform.ui.theme.sizes

const val MULTIPLE_CHOICE_LIST_TEST_TAG = "multiple choice items test tag"

@Composable
fun MultipleChoiceTaskScreen(viewModel: MultipleChoiceTaskViewModel, env: TaskScreenEnvironment) {
  val list by viewModel.items.collectAsStateWithLifecycle()
  val scrollState = rememberLazyListState()

  TaskContainer(viewModel = viewModel, dataCollectionViewModel = env.dataCollectionViewModel) {
    Box(modifier = Modifier.padding(horizontal = MaterialTheme.sizes.taskViewPadding)) {
      LazyColumn(Modifier.testTag(MULTIPLE_CHOICE_LIST_TEST_TAG), state = scrollState) {
        items(list, key = { it.option.id }) { item ->
          MultipleChoiceItemView(
            item = item,
            isLastIndex = list.indexOf(item) == list.lastIndex,
            toggleItem = { viewModel.onItemToggled(it) },
            otherValueChanged = { viewModel.onOtherTextChanged(it) },
          )
        }
      }
    }
  }
}
