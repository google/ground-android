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
package com.google.android.ground.domain.usecases.datasharingterms

import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.proto.Survey.DataSharingTerms
import com.google.android.ground.repository.SurveyRepository
import javax.inject.Inject

class GetDataSharingTermsUseCase
@Inject
constructor(
  private val localValueStore: LocalValueStore,
  private val surveyRepository: SurveyRepository,
) {

  /** Returns the data sharing terms for the currently active survey, if not already accepted. */
  operator fun invoke(): Result<DataSharingTerms?> = runCatching {
    val survey = surveyRepository.activeSurvey ?: error("No active survey")
    val sharingTerms = survey.dataSharingTerms
    if (sharingTerms == null || localValueStore.getDataSharingConsent(survey.id)) {
      // User previously agreed to the terms or data sharing terms are missing.
      return Result.success(null)
    }
    if (sharingTerms.type == DataSharingTerms.Type.CUSTOM && sharingTerms.customText.isBlank()) {
      throw InvalidCustomSharingTermsException()
    }
    return Result.success(sharingTerms)
  }

  class InvalidCustomSharingTermsException : Exception()
}
