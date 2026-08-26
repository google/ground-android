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

import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.repository.SurveyRepositoryInterface

/**
 * Returns the data sharing terms for the currently active survey, if not already accepted.
 *
 * Returns [Result.success] with `null` if the survey has no terms or the user has already accepted
 * them. Returns [Result.failure] with [InvalidCustomSharingTermsException] if custom terms text is
 * blank, or [IllegalStateException] if no survey is currently active.
 */
class GetDataSharingTermsUseCase(private val surveyRepository: SurveyRepositoryInterface) {

  operator fun invoke(): Result<Survey.DataSharingTerms?> = runCatching {
    val survey = surveyRepository.activeSurvey ?: error("No active survey")
    val sharingTerms = survey.dataSharingTerms
    if (sharingTerms == null || surveyRepository.getDataSharingConsent(survey.id)) {
      // User previously agreed to the terms or data sharing terms are missing.
      return Result.success(null)
    }
    if (sharingTerms is Survey.DataSharingTerms.Custom && sharingTerms.text.isBlank()) {
      throw InvalidCustomSharingTermsException()
    }
    return Result.success(sharingTerms)
  }

  /** Thrown when a survey defines custom data sharing terms with blank text. */
  class InvalidCustomSharingTermsException : Exception()
}
