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
import com.google.android.ground.R
import com.google.android.ground.databinding.QuestionDataCollectionFragBinding
import com.google.android.ground.model.submission.Response
import com.google.android.ground.model.submission.TextResponse
import com.google.android.ground.model.task.Task
import dagger.hilt.android.AndroidEntryPoint

/** Fragment allowing the user to answer questions to complete a task. */
@AndroidEntryPoint
class QuestionDataCollectionFragment constructor(private val task: Task) :
  DataCollectionTaskFragment() {
  // TODO(#1146): Use the task to determine what UI should be shown to the user here

  // TODO(#1146): Restore the text contents if the user returns to this fragment

  // TODO(#1146): use the isRequired field of the task to control whether or not the Next button is
  //  enabled. Also update the text field in some way to indicate the it is required.
  private lateinit var binding: QuestionDataCollectionFragBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = QuestionDataCollectionFragBinding.inflate(inflater, container, false)

    binding.task = task
    binding.lifecycleOwner = this

    return binding.root
  }

  override fun onContinueClicked(): Result<Response?> =
    if (isResponseValid() || !task.isRequired) {
      Result.success(TextResponse(binding.userResponseText.text.toString()))
    } else {
      Result.failure(
        IncompleteInputException(resources.getString(R.string.question_data_collection_no_response))
      )
    }

  private fun isResponseValid(): Boolean = !binding.userResponseText.text.isNullOrBlank()
}
