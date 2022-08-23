package com.sharedtest.persistence.local

import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.local.room.dao.SurveyDao
import com.google.android.ground.persistence.local.room.entity.SurveyEntity
import io.reactivex.Completable
import javax.inject.Inject

class LocalDataStoreHelper @Inject constructor(private val surveyDao: SurveyDao) {

    private fun Completable.commit() {
        this.test().assertComplete()
    }

    fun insertSurvey(survey: Survey) {
        surveyDao.insertOrUpdate(SurveyEntity.fromSurvey(survey)).commit()
    }
}