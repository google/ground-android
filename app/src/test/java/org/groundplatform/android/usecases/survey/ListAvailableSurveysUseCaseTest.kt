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
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.FAKE_GENERAL_ACCESS
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.toListItem
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.remote.FakeRemoteDataStore
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.NetworkStatus
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

  private fun setupSurveys(
    localSurveys: List<Survey>,
    remoteSurveys: List<Survey>,
    publicSurveys: List<Survey>,
  ) = runWithTestDispatcher {
    localSurveys.forEach { setupLocalSurvey(it) }
    fakeRemoteDataStore.surveys = remoteSurveys
    fakeRemoteDataStore.publicSurveys = publicSurveys
  }

  @Test
  fun `when network is available, should return remote survey list`() = runWithTestDispatcher {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))

    val remoteSurveys = listOf(SURVEY_1, SURVEY_2)
    val localSurveys = listOf(SURVEY_1, SURVEY_3)
    val publicSurveys = listOf(PUBLIC_SURVEY_A, PUBLIC_SURVEY_B)
    setupSurveys(localSurveys, remoteSurveys, publicSurveys)

    val result = listAvailableSurveysUseCase().first()

    assertThat(result)
      .isEqualTo(
        listOf(
          SURVEY_1.toListItem(availableOffline = true),
          SURVEY_2.toListItem(availableOffline = false),
          PUBLIC_SURVEY_A.toListItem(availableOffline = false),
          PUBLIC_SURVEY_B.toListItem(availableOffline = false),
          SURVEY_3.toListItem(availableOffline = true),
        )
      )
  }

  @Test
  fun `when network is unavailable, should return local survey list`() = runWithTestDispatcher {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.UNAVAILABLE))

    val remoteSurveys = listOf(SURVEY_1, SURVEY_2)
    val localSurveys = listOf(SURVEY_1, SURVEY_3)
    val publicSurveys = listOf(PUBLIC_SURVEY_A, PUBLIC_SURVEY_B)
    setupSurveys(localSurveys, remoteSurveys, publicSurveys)

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
    val publicSurveys = listOf(PUBLIC_SURVEY_A, PUBLIC_SURVEY_B)
    setupSurveys(localSurveys, remoteSurveys, publicSurveys)

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
          PUBLIC_SURVEY_A.toListItem(availableOffline = false),
          PUBLIC_SURVEY_B.toListItem(availableOffline = false),
          SURVEY_3.toListItem(availableOffline = true),
        )
      )
  }

  @Test
  fun `when remote survey is saved, should update offline status of that survey`() =
    runWithTestDispatcher {
      whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))

      val remoteSurveys = listOf(SURVEY_1, SURVEY_2)
      val localSurveys = emptyList<Survey>()
      val publicSurveys = listOf(PUBLIC_SURVEY_A, PUBLIC_SURVEY_B)
      setupSurveys(localSurveys, remoteSurveys, publicSurveys)

      val resultFlow = listAvailableSurveysUseCase()

      // Verify that remote surveys are loaded
      assertThat(resultFlow.first())
        .isEqualTo(
          listOf(
            SURVEY_1.toListItem(availableOffline = false),
            SURVEY_2.toListItem(availableOffline = false),
            PUBLIC_SURVEY_A.toListItem(availableOffline = false),
            PUBLIC_SURVEY_B.toListItem(availableOffline = false),
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
            PUBLIC_SURVEY_A.toListItem(availableOffline = false),
            PUBLIC_SURVEY_B.toListItem(availableOffline = false),
          )
        )
    }

  companion object {
    private val SURVEY_1 =
      Survey(
        id = "1",
        title = "Survey 1",
        description = "",
        jobMap = emptyMap(),
        generalAccess = FAKE_GENERAL_ACCESS,
      )
    private val SURVEY_2 =
      Survey(
        id = "2",
        title = "Survey 2",
        description = "",
        jobMap = emptyMap(),
        generalAccess = FAKE_GENERAL_ACCESS,
      )
    private val SURVEY_3 =
      Survey(
        id = "3",
        title = "Survey 3",
        description = "",
        jobMap = emptyMap(),
        generalAccess = FAKE_GENERAL_ACCESS,
      )

    private val PUBLIC_SURVEY_A =
      Survey(
        id = "A",
        title = "Public Survey 1",
        description = "",
        jobMap = emptyMap(),
        generalAccess = FAKE_GENERAL_ACCESS,
      )
    private val PUBLIC_SURVEY_B =
      Survey(
        id = "B",
        title = "Public Survey 2",
        description = "",
        jobMap = emptyMap(),
        generalAccess = FAKE_GENERAL_ACCESS,
      )
  }
}
