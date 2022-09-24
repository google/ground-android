/*
 * Copyright 2022 Google LLC
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
package com.sharedtest.persistence.local

import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.dao.SurveyDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import io.reactivex.Completable
import javax.inject.Inject

class LocalDataStoreHelper @Inject constructor(private val surveyDao: SurveyDao) {

  private fun Completable.commit() {
    this.test().assertComplete()
  }

  fun insertSurvey(survey: Survey) {
    surveyDao.insertOrUpdate(survey.toLocalDataStoreObject()).commit()
  }
}
