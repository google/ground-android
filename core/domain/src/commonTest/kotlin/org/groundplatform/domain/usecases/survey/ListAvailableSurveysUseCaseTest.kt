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

package org.groundplatform.domain.usecases.survey

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.toListItem
import org.groundplatform.domain.system.NetworkStatus
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeNetworkManager
import org.groundplatform.testing.FakeSurveyRepository
import org.groundplatform.testing.FakeUserRepository

class ListAvailableSurveysUseCaseTest {
  private val networkManager = FakeNetworkManager()
  private val surveyRepository = FakeSurveyRepository()
  private val userRepository = FakeUserRepository()
  private val listAvailableSurveysUseCase =
    ListAvailableSurveysUseCase(networkManager, surveyRepository, userRepository)

  @Test
  fun `when network is available, should return remote survey list`() = runTest {
    networkManager.setNetworkStatus(NetworkStatus.AVAILABLE)

    val local1 = FakeDataGenerator.newSurvey(id = "1", title = "Survey 1")
    val local3 = FakeDataGenerator.newSurvey(id = "3", title = "Survey 3")
    val remote1 =
      FakeDataGenerator.newSurveyListItem(id = "1", title = "Survey 1", availableOffline = false)
    val remote2 =
      FakeDataGenerator.newSurveyListItem(id = "2", title = "Survey 2", availableOffline = false)
    surveyRepository.offlineSurveys = listOf(local1, local3)
    surveyRepository.remoteListItems = listOf(remote1, remote2)

    val result = listAvailableSurveysUseCase().first()

    assertEquals(
      listOf(
        remote1.copy(availableOffline = true),
        remote2.copy(availableOffline = false),
        local3.toListItem(availableOffline = true),
      ),
      result,
    )
  }

  @Test
  fun `when network is unavailable, should return local survey list`() = runTest {
    networkManager.setNetworkStatus(NetworkStatus.UNAVAILABLE)

    val local1 = FakeDataGenerator.newSurvey(id = "1", title = "Survey 1")
    val local3 = FakeDataGenerator.newSurvey(id = "3", title = "Survey 3")
    val remote1 =
      FakeDataGenerator.newSurveyListItem(id = "1", title = "Survey 1", availableOffline = false)
    val remote2 =
      FakeDataGenerator.newSurveyListItem(id = "2", title = "Survey 2", availableOffline = false)
    surveyRepository.offlineSurveys = listOf(local1, local3)
    surveyRepository.remoteListItems = listOf(remote1, remote2)

    val result = listAvailableSurveysUseCase().first()

    assertEquals(
      listOf(
        local1.toListItem(availableOffline = true),
        local3.toListItem(availableOffline = true),
      ),
      result,
    )
  }

  @Test
  fun `when network is toggled, should switch to local or remote survey list`() = runTest {
    networkManager.setNetworkStatus(NetworkStatus.UNAVAILABLE)

    val local1 = FakeDataGenerator.newSurvey(id = "1", title = "Survey 1")
    val local3 = FakeDataGenerator.newSurvey(id = "3", title = "Survey 3")
    val remote1 =
      FakeDataGenerator.newSurveyListItem(id = "1", title = "Survey 1", availableOffline = false)
    val remote2 =
      FakeDataGenerator.newSurveyListItem(id = "2", title = "Survey 2", availableOffline = false)
    surveyRepository.offlineSurveys = listOf(local1, local3)
    surveyRepository.remoteListItems = listOf(remote1, remote2)

    val flow = listAvailableSurveysUseCase()

    assertEquals(
      listOf(
        local1.toListItem(availableOffline = true),
        local3.toListItem(availableOffline = true),
      ),
      flow.first(),
    )

    networkManager.setNetworkStatus(NetworkStatus.AVAILABLE)

    assertEquals(
      listOf(
        remote1.copy(availableOffline = true),
        remote2.copy(availableOffline = false),
        local3.toListItem(availableOffline = true),
      ),
      flow.first(),
    )
  }

  @Test
  fun `when remote survey is saved, should update offline status of that survey`() = runTest {
    networkManager.setNetworkStatus(NetworkStatus.AVAILABLE)

    val survey1 = FakeDataGenerator.newSurvey(id = "1", title = "Survey 1")
    val remote1 =
      FakeDataGenerator.newSurveyListItem(id = "1", title = "Survey 1", availableOffline = false)
    val remote2 =
      FakeDataGenerator.newSurveyListItem(id = "2", title = "Survey 2", availableOffline = false)
    surveyRepository.offlineSurveys = emptyList()
    surveyRepository.remoteListItems = listOf(remote1, remote2)

    val flow = listAvailableSurveysUseCase()

    assertEquals(
      listOf(
        remote1.copy(availableOffline = false),
        remote2.copy(availableOffline = false),
      ),
      flow.first(),
    )

    surveyRepository.saveSurvey(survey1)

    assertEquals(
      listOf(
        remote1.copy(availableOffline = true),
        remote2.copy(availableOffline = false),
      ),
      flow.first(),
    )
  }
}
