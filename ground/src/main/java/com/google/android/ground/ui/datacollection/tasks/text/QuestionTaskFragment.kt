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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.databinding.QuestionTaskFragBinding
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewWithHeader
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.datacollection.tasks.TaskFragment.Companion.POSITION
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

/** Fragment allowing the user to answer questions to complete a task. */
@AndroidEntryPoint
class QuestionTaskFragment : AbstractTaskFragment<TextTaskViewModel>() {
  override lateinit var viewModel: TextTaskViewModel
  override var position by Delegates.notNull<Int>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      position = savedInstanceState.getInt(POSITION)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(POSITION, position)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    viewModel = dataCollectionViewModel.getTaskViewModel(position) as TextTaskViewModel

    // Base template with header and footer
    taskView = TaskViewWithHeader.create(container, inflater, this, viewModel)

    // Task view
    val taskBinding = QuestionTaskFragBinding.inflate(inflater)
    taskBinding.viewModel = viewModel
    taskBinding.lifecycleOwner = this
    taskView.addTaskView(taskBinding.root)

    return taskView.root
  }
}
