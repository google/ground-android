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
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.R
import com.google.android.ground.model.submission.Response
import com.google.android.ground.model.task.Task
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import java8.util.Optional

/** Defines the state of an inflated [Task] and controls its UI.  */
open class AbstractTaskViewModel internal constructor(private val resources: Resources) :
    AbstractViewModel() {

    /** Current value. */
    val response: LiveData<Optional<Response>>

    /** Transcoded text to be displayed for the current [AbstractTaskViewModel.response]. */
    val responseText: LiveData<String>

    /** Error message to be displayed for the current [AbstractTaskViewModel.response]. */
    val error: @Hot(replays = true) MutableLiveData<String> = MutableLiveData()

    private val responseSubject: @Hot(replays = true) BehaviorProcessor<Optional<Response>> =
        BehaviorProcessor.create()

    protected lateinit var task: Task

    // TODO: Add a reference of Task in Response for simplification.
    fun initialize(task: Task, response: Optional<Response>) {
        this.task = task
        setResponse(response)
    }

    protected val detailsTextFlowable: @Cold(stateful = true, terminates = false) Flowable<String> =
        responseSubject
            .distinctUntilChanged()
            .map { responseOptional: Optional<Response> ->
                responseOptional.map { it.getDetailsText() }.orElse("")
            }

    /** Checks if the current response is valid and updates error value. */
    fun validate(): Optional<String> {
        val result = validate(task, responseSubject.value)
        error.postValue(result.orElse(null))
        return result
    }

    // TODO: Check valid response values
    private fun validate(task: Task, response: Optional<Response>?): Optional<String> =
        if (task.isRequired && (response == null || response.isEmpty))
            Optional.of(resources.getString(R.string.required_task))
        else
            Optional.empty()

    fun taskLabel(): String =
        StringBuilder(task.label).apply {
            if (task.isRequired) {
                append(" *")
            }
        }.toString()

    fun setResponse(response: Optional<Response>) {
        responseSubject.onNext(response)
    }

    fun clearResponse() {
        setResponse(Optional.empty())
    }

    init {
        response = LiveDataReactiveStreams.fromPublisher(responseSubject.distinctUntilChanged())
        responseText = LiveDataReactiveStreams.fromPublisher(detailsTextFlowable)
    }
}