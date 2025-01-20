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
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Coordinates persistence and retrieval of [Survey] instances from remote, local, and in memory
 * data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SurveyRepository
@Inject
constructor(
  private val firebaseCrashLogger: FirebaseCrashLogger,
  private val localSurveyStore: LocalSurveyStore,
  private val localValueStore: LocalValueStore,
  @ApplicationScope private val externalScope: CoroutineScope,
) {
  private val _selectedSurveyId = MutableStateFlow<String?>(null)

  val activeSurveyFlow: StateFlow<Survey?> =
    _selectedSurveyId
      .flatMapLatest { id -> getOfflineSurveyFlow(id) }
      .stateIn(externalScope, SharingStarted.Lazily, null)

  /** The currently active survey, or `null` if no survey is active. */
  val activeSurvey: Survey?
    get() = activeSurveyFlow.value

  /** The id of the last activated survey. */
  private var lastActiveSurveyId: String by localValueStore::lastActiveSurveyId

  init {
    activeSurveyFlow.filterNotNull().onEach { lastActiveSurveyId = it.id }.launchIn(externalScope)
  }

  /**
   * Returns the survey with the specified id from the local db, or `null` if not available offline.
   */
  suspend fun getOfflineSurvey(surveyId: String): Survey? = localSurveyStore.getSurveyById(surveyId)

  private fun getOfflineSurveyFlow(id: String?): Flow<Survey?> =
    if (id.isNullOrBlank()) flowOf(null) else localSurveyStore.survey(id)

  fun activateSurvey(surveyId: String) {
    _selectedSurveyId.update { surveyId }
    firebaseCrashLogger.setSelectedSurveyId(surveyId)
  }

  fun clearActiveSurvey() {
    activateSurvey("")
  }

  // TODO: Use activeSurvey instead of selectedSurveyId as it is possible to have no active survey.
  // Issue URL: https://github.com/google/ground-android/issues/3020
  fun hasActiveSurvey(): Boolean = _selectedSurveyId.value?.isNotBlank() ?: false

  fun isSurveyActive(surveyId: String): Boolean =
    surveyId.isNotBlank() && activeSurvey?.id == surveyId

  /** Attempts to remove the locally synced survey. Doesn't throw an error if it doesn't exist. */
  suspend fun removeOfflineSurvey(surveyId: String) {
    val survey = localSurveyStore.getSurveyById(surveyId)
    survey?.let { localSurveyStore.deleteSurvey(survey) }
    if (isSurveyActive(surveyId)) {
      clearActiveSurvey()
    }
  }
}
