/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.ui.editsubmission

import android.content.res.Resources
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData.Companion.fromList
import com.google.android.ground.model.task.Option
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import java8.util.Optional
import javax.inject.Inject

class MultipleChoiceTaskViewModel @Inject constructor(resources: Resources) :
  AbstractDialogTaskViewModel(resources) {

  fun getCurrentResponse(): Optional<MultipleChoiceTaskData> =
    taskData.value?.map { it as MultipleChoiceTaskData } ?: Optional.empty()

  fun updateResponse(options: ImmutableList<Option>) {
    setResponse(fromList(task.multipleChoice, options.map(Option::id).toImmutableList()))
  }
}
