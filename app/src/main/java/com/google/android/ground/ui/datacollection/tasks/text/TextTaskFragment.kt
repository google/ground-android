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
package com.google.android.ground.ui.datacollection.tasks.text

import android.view.LayoutInflater
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.google.android.ground.model.submission.TextTaskData.Companion.fromString
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.components.TextTaskInput
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.util.createComposeView
import dagger.hilt.android.AndroidEntryPoint

const val INPUT_TEXT_TEST_TAG: String = "text task input test tag"

/** Fragment allowing the user to answer questions to complete a task. */
@AndroidEntryPoint
class TextTaskFragment : AbstractTaskFragment<TextTaskViewModel>() {

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View = createComposeView {
    ShowTextInputField()
  }

  @Composable
  private fun ShowTextInputField() {
    val userResponse by viewModel.responseText.observeAsState("")
    TextTaskInput(userResponse, modifier = Modifier.testTag(INPUT_TEXT_TEST_TAG)) { newText ->
      viewModel.setValue(fromString(newText))
    }
  }
}
