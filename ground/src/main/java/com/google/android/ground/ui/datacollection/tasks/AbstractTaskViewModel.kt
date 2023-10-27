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
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.R
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.model.task.Task
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import java8.util.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Defines the state of an inflated [Task] and controls its UI. */
open class AbstractTaskViewModel internal constructor(private val resources: Resources) :
  AbstractViewModel() {

  /** Current value. */
  @Deprecated("Use taskDataValue instead") val taskData: LiveData<Optional<TaskData>>

  val taskDataFlow: MutableStateFlow<TaskData?> = MutableStateFlow(null)

  // TODO(Shobhit): Rename to taskData once legacy taskData is cleaned up.
  val taskDataValue: StateFlow<TaskData?> =
    taskDataFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

  /** Transcoded text to be displayed for the current [AbstractTaskViewModel.taskData]. */
  val responseText: LiveData<String>

  /** Error message to be displayed for the current [AbstractTaskViewModel.taskData]. */
  val error: @Hot(replays = true) MutableLiveData<String> = MutableLiveData()

  private val taskDataSubject: @Hot(replays = true) BehaviorProcessor<Optional<TaskData>> =
    BehaviorProcessor.create()

  lateinit var task: Task

  init {
    taskData = taskDataSubject.distinctUntilChanged().startWith(Optional.empty()).toLiveData()
    responseText = detailsTextFlowable().toLiveData()
  }

  // TODO: Add a reference of Task in TaskData for simplification.
  open fun initialize(job: Job, task: Task, taskData: TaskData?) {
    this.task = task
    setResponse(taskData)
  }

  private fun detailsTextFlowable(): @Cold(stateful = true, terminates = false) Flowable<String> =
    taskDataSubject.distinctUntilChanged().map { taskDataOptional: Optional<TaskData> ->
      taskDataOptional.map { it.getDetailsText() }.orElse("")
    }

  /** Checks if the current taskData is valid and updates error value. */
  fun validate(): String? {
    val result = validate(task, taskDataSubject.value).orElse(null)
    error.postValue(result)
    return result
  }

  // TODO: Check valid taskData values
  private fun validate(task: Task, taskData: Optional<TaskData>?): Optional<String> =
    if (task.isRequired && (taskData == null || taskData.isEmpty))
      Optional.of(resources.getString(R.string.required_task))
    else Optional.empty()

  fun setResponse(taskData: TaskData?) {
    taskDataSubject.onNext(Optional.ofNullable(taskData))
    taskDataFlow.value = taskData
  }

  open fun clearResponse() {
    setResponse(null)
  }

  fun isTaskOptional(): Boolean = !task.isRequired

  fun hasNoData(): Boolean = taskDataFlow.value.isNullOrEmpty()
}
