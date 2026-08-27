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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.model.toListItem
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.domain.system.NetworkManagerInterface
import org.groundplatform.domain.system.NetworkStatus

/**
 * Returns a flow of [SurveyListItem]s available to the current user.
 *
 * When network connectivity is available, lists remote surveys merged with local offline
 * availability status. When offline, lists only local surveys available on the device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListAvailableSurveysUseCase(
  private val networkManager: NetworkManagerInterface,
  private val surveyRepository: SurveyRepositoryInterface,
  private val userRepository: UserRepositoryInterface,
) {

  operator fun invoke(): Flow<List<SurveyListItem>> =
    networkManager.networkStatusFlow.flatMapLatest { networkStatus ->
      if (networkStatus == NetworkStatus.AVAILABLE) {
        getRemoteSurveyList()
      } else {
        getLocalSurveyList()
      }
    }

  private fun getLocalSurveyList(): Flow<List<SurveyListItem>> =
    surveyRepository.getOfflineSurveys().map { localSurveys ->
      localSurveys.map { localSurvey -> localSurvey.toListItem(true) }
    }

  private suspend fun getRemoteSurveyList(): Flow<List<SurveyListItem>> {
    val user = userRepository.getAuthenticatedUser()
    val remoteSurveyFlow = surveyRepository.getRemoteSurveys(user)

    return combine(remoteSurveyFlow, getLocalSurveyList()) { remoteSurveys, localSurveys ->
      val localSurveyIds = localSurveys.map { it.id }.toSet()
      val remoteSurveyIds = remoteSurveys.map { it.id }.toSet()

      val remoteSurveysWithOfflineStatus = remoteSurveys.map { remoteSurvey ->
        remoteSurvey.copy(availableOffline = remoteSurvey.id in localSurveyIds)
      }
      val localOnlySurveys = localSurveys.filter { it.id !in remoteSurveyIds }

      remoteSurveysWithOfflineStatus + localOnlySurveys
    }
  }
}
