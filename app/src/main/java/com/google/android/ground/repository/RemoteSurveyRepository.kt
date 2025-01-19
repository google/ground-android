package com.google.android.ground.repository

import com.google.android.ground.model.Survey
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.model.User
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.persistence.remote.firebase.FirebaseMessagingService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val LOAD_REMOTE_SURVEY_TIMEOUT_MILLS: Long = 15 * 1000

@Singleton
class RemoteSurveyRepository @Inject constructor(private val remoteDataStore: RemoteDataStore) {

  suspend fun fetchSurvey(surveyId: String): Survey? =
    withTimeoutOrNull(LOAD_REMOTE_SURVEY_TIMEOUT_MILLS) {
      Timber.d("Loading survey $surveyId")
      remoteDataStore.loadSurvey(surveyId)
    }

  /** Attempts to load all surveys that the user has access to. */
  fun fetchAllReadableSurveysFlow(user: User): Flow<List<SurveyListItem>> =
    remoteDataStore.getSurveyList(user)

  /**
   * Listens for remote changes to the survey with the specified id. [FirebaseMessagingService]
   * listens to the updates and enqueues a worker to sync the survey to local storage.
   */
  suspend fun subscribeToSurveyUpdates(surveyId: String) =
    remoteDataStore.subscribeToSurveyUpdates(surveyId)
}
