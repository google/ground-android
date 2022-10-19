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
package com.google.android.ground.ui.datacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.BR
import com.google.android.ground.databinding.QuestionDataCollectionFragBinding
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.editsubmission.AbstractTaskViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Fragment allowing the user to answer questions to complete a task. */
@AndroidEntryPoint
class QuestionDataCollectionFragment
constructor(private val task: Task, private val viewModel: AbstractTaskViewModel) :
  AbstractFragment() {
  // TODO(#1146): Use the task to determine what UI should be shown to the user here

  // TODO(#1146): Persist the text contents when the user clicks next

  // TODO(#1146): Restore the text contents if the user returns to this fragment

  // TODO(#1146): use the isRequired field of the task to control whether or not the Next button is
  //  enabled. Also update the text field in some way to indicate the it is required.

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = QuestionDataCollectionFragBinding.inflate(inflater, container, false)

    binding.lifecycleOwner = this
    binding.setVariable(BR.viewModel, viewModel)

    return binding.root
  }
}
