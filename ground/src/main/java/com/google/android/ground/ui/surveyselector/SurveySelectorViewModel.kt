/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.ui.surveyselector

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.google.android.ground.model.Survey
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.Loadable
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.common.collect.ImmutableList
import io.reactivex.Single
import javax.inject.Inject
import timber.log.Timber

/** Represents view state and behaviors of the survey selector dialog. */
class SurveySelectorViewModel
@Inject
internal constructor(
  private val surveyRepository: SurveyRepository,
  authManager: AuthenticationManager
) : AbstractViewModel() {
  val surveySummaries: LiveData<Loadable<List<Survey>>?>
  val offlineSurveys: Single<ImmutableList<Survey>>
    get() = surveyRepository.offlineSurveys

  /**
   * Triggers the specified survey to be loaded and activated.
   *
   * @param idx the index in the survey summary list.
   */
  fun activateSurvey(idx: Int) {
    val surveys: List<Survey> = surveySummaries.value?.value()?.orElse(listOf()) ?: listOf()
    if (surveys.isEmpty()) {
      Timber.e("Can't activate survey before list is loaded")
      return
    }
    if (idx >= surveys.size) {
      Timber.e("Can't activate survey at index $idx, only ${surveys.size} surveys in list")
      return
    }
    val (id) = surveys[idx]
    surveyRepository.activateSurvey(id)
  }

  fun activateOfflineSurvey(surveyId: String?) {
    surveyRepository.activateSurvey(surveyId!!)
  }

  init {
    surveySummaries =
      LiveDataReactiveStreams.fromPublisher(
        surveyRepository.getSurveySummaries(authManager.currentUser)
      )
  }
}
