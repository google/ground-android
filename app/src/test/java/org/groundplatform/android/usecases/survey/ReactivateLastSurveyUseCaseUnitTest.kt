/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.usecases.survey

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.repository.SurveyRepository
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class ReactivateLastSurveyUseCaseUnitTest {

  private val activateSurvey = mock(ActivateSurveyUseCase::class.java)
  private val localValueStore = mock(LocalValueStore::class.java)
  private val surveyRepository = mock(SurveyRepository::class.java)
  private val reactivateLastSurvey =
    ReactivateLastSurveyUseCase(activateSurvey, localValueStore, surveyRepository)

  @Test
  fun `when activateSurvey throws exception, returns false`() = runBlocking {
    `when`(localValueStore.lastActiveSurveyId).thenReturn("survey_id")
    `when`(surveyRepository.activeSurvey).thenReturn(null)
    `when`(activateSurvey.invoke(any())).thenThrow(RuntimeException("Network error"))

    val result = reactivateLastSurvey()

    assertThat(result).isFalse()
  }

  @Test
  fun `when activateSurvey succeeds, returns true`() = runBlocking {
    `when`(localValueStore.lastActiveSurveyId).thenReturn("survey_id")
    `when`(surveyRepository.activeSurvey).thenReturn(null)
    `when`(activateSurvey.invoke(any())).thenReturn(true)

    val result = reactivateLastSurvey()

    assertThat(result).isTrue()
  }
}
