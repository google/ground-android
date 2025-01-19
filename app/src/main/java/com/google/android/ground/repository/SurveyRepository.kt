/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.repository

import com.google.android.ground.FirebaseCrashLogger
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.local.LocalValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Maintains a state of currently active [Survey] in the application. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SurveyRepository
@Inject
constructor(
  private val firebaseCrashLogger: FirebaseCrashLogger,
  private val localValueStore: LocalValueStore,
  @ApplicationScope private val externalScope: CoroutineScope,
  private val localSurveyRepository: LocalSurveyRepository,
) {

  private val _selectedSurveyIdFlow = MutableStateFlow<String?>(null)

  var selectedSurveyId: String?
    get() = _selectedSurveyIdFlow.value
    set(value) {
      _selectedSurveyIdFlow.update { value }
    }

  val activeSurveyFlow: StateFlow<Survey?> =
    _selectedSurveyIdFlow
      .flatMapLatest { surveyId ->
        surveyId?.let { localSurveyRepository.loadSurveyFlow(it) } ?: flowOf(null)
      }
      .onEach { survey ->
        val surveyId = survey?.id ?: ""
        localValueStore.lastActiveSurveyId = surveyId
        firebaseCrashLogger.setSelectedSurveyId(surveyId)
      }
      .stateIn(externalScope, SharingStarted.Lazily, null)

  /** The currently active survey, or `null` if no survey is active. */
  val activeSurvey: Survey?
    get() = activeSurveyFlow.value

  fun clearActiveSurvey() {
    selectedSurveyId = null
  }
}
