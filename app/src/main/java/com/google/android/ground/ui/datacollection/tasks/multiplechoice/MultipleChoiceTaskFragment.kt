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
package com.google.android.ground.ui.datacollection.tasks.multiplechoice

import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.asLiveData
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.util.createComposeView
import dagger.hilt.android.AndroidEntryPoint

const val MULTIPLE_CHOICE_LIST_TEST_TAG = "multiple choice items test tag"

/**
 * Fragment allowing the user to answer single selection multiple choice questions to complete a
 * task.
 */
@AndroidEntryPoint
class MultipleChoiceTaskFragment : AbstractTaskFragment<MultipleChoiceTaskViewModel>() {

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View = createComposeView {
    ShowMultipleChoiceItems()
  }

  @Composable
  private fun ShowMultipleChoiceItems() {
    val list by viewModel.itemsFlow.asLiveData().observeAsState()
    list?.let { items ->
      LazyColumn(Modifier.fillMaxSize().testTag(MULTIPLE_CHOICE_LIST_TEST_TAG)) {
        items(items) { item ->
          MultipleChoiceItemView(
            item = item,
            isLastIndex = items.indexOf(item) == items.lastIndex,
            toggleItem = { viewModel.onItemToggled(it) },
            otherValueChanged = { viewModel.onOtherTextChanged(it) },
          )
        }
      }
    }
  }
}
