/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.domain.usecases.survey

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ReactivateLastSurveyUseCaseTest : BaseHiltTest() {

  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localValueStore: LocalValueStore
  @Inject lateinit var reactivateLastSurvey: ReactivateLastSurveyUseCase

  @Test
  fun `when last survey id is present, should activate it`() = runWithTestDispatcher {
    localValueStore.lastActiveSurveyId = SURVEY_ID
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    val result = reactivateLastSurvey()
    advanceUntilIdle()

    assertThat(result).isTrue()
  }

  @Test
  fun `when survey is already active, should do nothing`() = runWithTestDispatcher {
    localValueStore.lastActiveSurveyId = SURVEY_ID
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    activateSurvey(SURVEY_ID)

    val result = reactivateLastSurvey()
    advanceUntilIdle()

    assertThat(result).isTrue()
  }

  @Test
  fun `when last survey id is not present, should do nothing`() = runWithTestDispatcher {
    localValueStore.lastActiveSurveyId = ""
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    assertThat(reactivateLastSurvey()).isFalse()
  }

  @Test
  fun `when last survey id is present but survey is not present, should do nothing`() {
    localValueStore.lastActiveSurveyId = SURVEY_ID

    assertFails { runWithTestDispatcher { reactivateLastSurvey() } }
  }

  companion object {
    private const val SURVEY_ID = "survey_id"

    private val SURVEY =
      Survey(
        id = SURVEY_ID,
        title = "survey title",
        description = "survey description",
        jobMap = emptyMap(),
      )
  }
}
