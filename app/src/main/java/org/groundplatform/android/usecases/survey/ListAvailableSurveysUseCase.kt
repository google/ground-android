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

import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.model.toListItem
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.remote.RemoteDataStore
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.NetworkStatus

/** Returns a flow of [SurveyListItem] to be displayed to the user. */
@OptIn(ExperimentalCoroutinesApi::class)
class ListAvailableSurveysUseCase
@Inject
constructor(
  private val localSurveyStore: LocalSurveyStore,
  private val networkManager: NetworkManager,
  private val remoteDataStore: RemoteDataStore,
  private val userRepository: UserRepository,
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
    localSurveyStore.surveys.map { localSurveys ->
      localSurveys.map { localSurvey -> localSurvey.toListItem(true) }
    }

  private suspend fun getRemoteSurveyList(): Flow<List<SurveyListItem>> {
    val user = userRepository.getAuthenticatedUser()
    val remoteSurveyListFlow = remoteDataStore.getSurveyList(user)
    return remoteSurveyListFlow.combine(getLocalSurveyList()) { remoteSurveys, localSurveys ->
      remoteSurveys.map { remoteSurvey -> addOfflineStatus(remoteSurvey, localSurveys) }
    }
  }

  private fun addOfflineStatus(remoteSurvey: SurveyListItem, localSurveys: List<SurveyListItem>) =
    remoteSurvey.copy(availableOffline = localSurveys.any { it.id == remoteSurvey.id })
}
