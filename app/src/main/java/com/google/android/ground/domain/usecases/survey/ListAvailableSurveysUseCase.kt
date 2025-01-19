package com.google.android.ground.domain.usecases.survey

import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.repository.LocalSurveyRepository
import com.google.android.ground.repository.RemoteSurveyRepository
import com.google.android.ground.system.NetworkManager
import com.google.android.ground.system.NetworkStatus
import com.google.android.ground.system.auth.AuthenticationManager
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ListAvailableSurveysUseCase
@Inject
constructor(
  private val authManager: AuthenticationManager,
  private val localSurveyRepository: LocalSurveyRepository,
  private val networkManager: NetworkManager,
  private val remoteSurveyRepository: RemoteSurveyRepository,
) {

  operator fun invoke(): Flow<List<SurveyListItem>> {
    val localSurveysFlow = localSurveyRepository.loadAllSurveysFlow()
    return networkManager.networkStatusFlow.flatMapLatest { status ->
      if (status == NetworkStatus.AVAILABLE) {
        fetchRemoteSurveysWithOfflineStatusFlow(localSurveysFlow)
      } else {
        localSurveysFlow
      }
    }
  }

  private suspend fun fetchRemoteSurveysWithOfflineStatusFlow(
    localSurveysFlow: Flow<List<SurveyListItem>>
  ): Flow<List<SurveyListItem>> {
    val loggedInUser = authManager.getAuthenticatedUser()
    val remoteSurveysFlow = remoteSurveyRepository.fetchAllReadableSurveysFlow(loggedInUser)
    return remoteSurveysFlow.combine(localSurveysFlow) { remoteSurveys, offlineSurveys ->
      remoteSurveys.map { it.addOfflineStatus(offlineSurveys) }
    }
  }

  private fun SurveyListItem.addOfflineStatus(offlineSurveys: List<SurveyListItem>) =
    copy(availableOffline = offlineSurveys.any { it.id == id })
}
