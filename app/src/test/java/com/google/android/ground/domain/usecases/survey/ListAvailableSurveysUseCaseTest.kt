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
package com.google.android.ground.domain.usecases.survey

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.Survey
import com.google.android.ground.model.toListItem
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.remote.FakeRemoteDataStore
import com.google.android.ground.system.NetworkManager
import com.google.android.ground.system.NetworkStatus
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ListAvailableSurveysUseCaseTest : BaseHiltTest() {

  @BindValue @Mock lateinit var networkManager: NetworkManager

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var listAvailableSurveysUseCase: ListAvailableSurveysUseCase
  @Inject lateinit var localSurveyStore: LocalSurveyStore

  private fun setupLocalSurvey(survey: Survey) = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(survey)
  }

  private fun setupSurveys(localSurveys: List<Survey>, remoteSurveys: List<Survey>) =
    runWithTestDispatcher {
      localSurveys.forEach { setupLocalSurvey(it) }
      fakeRemoteDataStore.surveys = remoteSurveys
    }

  @Test
  fun `when network is available, should return remote survey list`() = runWithTestDispatcher {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))

    val remoteSurveys = listOf(SURVEY_1, SURVEY_2)
    val localSurveys = listOf(SURVEY_1, SURVEY_3)
    setupSurveys(localSurveys, remoteSurveys)

    val result = listAvailableSurveysUseCase().first()

    assertThat(result)
      .isEqualTo(
        listOf(
          SURVEY_1.toListItem(availableOffline = true),
          SURVEY_2.toListItem(availableOffline = false),
        )
      )
  }

  @Test
  fun `when network is unavailable, should return local survey list`() = runWithTestDispatcher {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.UNAVAILABLE))

    val remoteSurveys = listOf(SURVEY_1, SURVEY_2)
    val localSurveys = listOf(SURVEY_1, SURVEY_3)
    setupSurveys(localSurveys, remoteSurveys)

    val result = listAvailableSurveysUseCase().first()

    assertThat(result)
      .isEqualTo(
        listOf(
          SURVEY_1.toListItem(availableOffline = true),
          SURVEY_3.toListItem(availableOffline = true),
        )
      )
  }

  @Test
  fun `when network is toggled, should return local survey list`() = runWithTestDispatcher {
    val mutableNetworkStatusFlow = MutableStateFlow(NetworkStatus.UNAVAILABLE)
    whenever(networkManager.networkStatusFlow).thenReturn(mutableNetworkStatusFlow)

    val remoteSurveys = listOf(SURVEY_1, SURVEY_2)
    val localSurveys = listOf(SURVEY_1, SURVEY_3)
    setupSurveys(localSurveys, remoteSurveys)

    val resultFlow = listAvailableSurveysUseCase()

    // Verify that survey list is loaded from local storage
    assertThat(resultFlow.first())
      .isEqualTo(
        listOf(
          SURVEY_1.toListItem(availableOffline = true),
          SURVEY_3.toListItem(availableOffline = true),
        )
      )

    // Change network status
    mutableNetworkStatusFlow.emit(NetworkStatus.AVAILABLE)

    // Verify that survey list is now updated
    assertThat(resultFlow.first())
      .isEqualTo(
        listOf(
          SURVEY_1.toListItem(availableOffline = true),
          SURVEY_2.toListItem(availableOffline = false),
        )
      )
  }

  @Test
  fun `when remote survey is saved, should update offline status of that survey`() =
    runWithTestDispatcher {
      whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))

      val remoteSurveys = listOf(SURVEY_1, SURVEY_2)
      val localSurveys = emptyList<Survey>()
      setupSurveys(localSurveys, remoteSurveys)

      val resultFlow = listAvailableSurveysUseCase()

      // Verify that remote surveys are loaded
      assertThat(resultFlow.first())
        .isEqualTo(
          listOf(
            SURVEY_1.toListItem(availableOffline = false),
            SURVEY_2.toListItem(availableOffline = false),
          )
        )

      // Save survey locally
      setupLocalSurvey(SURVEY_1)

      // Verify that survey list is now updated
      assertThat(resultFlow.first())
        .isEqualTo(
          listOf(
            SURVEY_1.toListItem(availableOffline = true),
            SURVEY_2.toListItem(availableOffline = false),
          )
        )
    }

  companion object {
    private val SURVEY_1 =
      Survey(id = "1", title = "Survey 1", description = "", jobMap = emptyMap())
    private val SURVEY_2 =
      Survey(id = "2", title = "Survey 2", description = "", jobMap = emptyMap())
    private val SURVEY_3 =
      Survey(id = "3", title = "Survey 3", description = "", jobMap = emptyMap())
  }
}
