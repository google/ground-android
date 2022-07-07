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
package com.google.android.gnd.ui.datacollection

import androidx.lifecycle.viewModelScope
import com.google.android.gnd.model.submission.Submission
import com.google.android.gnd.repository.SubmissionRepository
import com.google.android.gnd.ui.common.AbstractViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import javax.inject.Inject

/**
 * View model for the Data Collection fragment.
 */
class DataCollectionViewModel @Inject internal constructor(private val submissionRepository: SubmissionRepository) :
    AbstractViewModel() {
    // Backing property to avoid state updates from other classes
    private val _submission = MutableStateFlow<Submission?>(null)

    val submission: StateFlow<Submission?> = _submission

    fun load(
        surveyId: String,
        locationOfInterestId: String,
        submissionId: String
    ) =
        viewModelScope.launch {
            _submission.value =
                submissionRepository.createSubmission(surveyId, locationOfInterestId, submissionId)
                    .await()
        }
}