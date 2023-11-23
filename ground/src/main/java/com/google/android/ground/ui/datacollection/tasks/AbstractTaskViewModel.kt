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
package com.google.android.ground.ui.datacollection.tasks

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.R
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.model.task.Task
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import java8.util.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** Defines the state of an inflated [Task] and controls its UI. */
open class AbstractTaskViewModel internal constructor(private val resources: Resources) :
  AbstractViewModel() {

  /** Current value. */
  private val _valueFlow: MutableStateFlow<Value?> = MutableStateFlow(null)
  val taskValue: StateFlow<Value?> = _valueFlow.asStateFlow()

  /** Transcoded text to be displayed for the current [Value]. */
  val responseText: LiveData<String>

  /** Error message to be displayed for the current [Value]. */
  val error: @Hot(replays = true) MutableLiveData<String> = MutableLiveData()

  lateinit var task: Task

  init {
    responseText = detailsTextFlow().asLiveData()
  }

  open fun initialize(job: Job, task: Task, value: Value?) {
    this.task = task
    setValue(value)
  }

  private fun detailsTextFlow(): Flow<String> = taskValue.map { it?.getDetailsText() ?: "" }

  /** Checks if the current value is valid and updates error value. */
  fun validate(): String? {
    val result = validate(task, taskValue.value).orElse(null)
    error.postValue(result)
    return result
  }

  // TODO: Check valid values.
  private fun validate(task: Task, value: Value?): Optional<String> =
    if (task.isRequired && (value == null || value.isEmpty()))
      Optional.of(resources.getString(R.string.required_task))
    else Optional.empty()

  fun setValue(value: Value?) {
    _valueFlow.value = value
  }

  open fun clearResponse() {
    setValue(null)
  }

  fun isTaskOptional(): Boolean = !task.isRequired

  fun hasNoData(): Boolean = taskValue.value.isNullOrEmpty()
}
