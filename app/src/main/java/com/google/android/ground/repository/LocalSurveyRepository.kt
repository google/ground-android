package com.google.android.ground.repository

import com.google.android.ground.model.Survey
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.model.toListItem
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSurveyRepository @Inject constructor(private val localSurveyStore: LocalSurveyStore) {

  suspend fun loadSurvey(surveyId: String): Survey? = localSurveyStore.getSurveyById(surveyId)

  fun loadSurveyFlow(surveyId: String): Flow<Survey?> = localSurveyStore.survey(surveyId)

  fun loadAllSurveysFlow(): Flow<List<SurveyListItem>> =
    localSurveyStore.surveys.map { surveys ->
      surveys.map { it.toListItem(availableOffline = true) }
    }

  suspend fun saveSurvey(survey: Survey) {
    localSurveyStore.insertOrUpdateSurvey(survey)
  }

  suspend fun removeSurvey(surveyId: String) {
    localSurveyStore.getSurveyById(surveyId)?.let { survey ->
      localSurveyStore.deleteSurvey(survey)
    }
  }
}
