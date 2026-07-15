/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.SubmissionRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.domain.usecases.GetLoiReportUseCase
import org.groundplatform.domain.usecases.submission.SubmitDataUseCase
import org.groundplatform.domain.usecases.survey.GetSurveyListItemUseCase
import org.groundplatform.domain.usecases.survey.SyncSurveyUseCase
import org.groundplatform.domain.usecases.user.GetUserSettingsUseCase
import org.groundplatform.domain.usecases.user.UpdateUserSettingsUseCase
import org.groundplatform.ui.util.DateFormatter

@InstallIn(SingletonComponent::class)
@Module
object UseCaseModule {
  @Provides
  fun provideGetLoiReportUseCase(
    locationOfInterestRepository: LocationOfInterestRepositoryInterface,
    userRepository: UserRepositoryInterface,
    surveyRepository: SurveyRepositoryInterface,
    submissionRepository: SubmissionRepositoryInterface,
    dateFormatter: DateFormatter,
  ) =
    GetLoiReportUseCase(
      locationOfInterestRepository = locationOfInterestRepository,
      userRepositoryInterface = userRepository,
      surveyRepositoryInterface = surveyRepository,
      submissionRepositoryInterface = submissionRepository,
      formatDateTime = dateFormatter::formatDateTime,
    )

  @Provides
  fun providesUpdateUserSettingsUseCase(userRepository: UserRepositoryInterface) =
    UpdateUserSettingsUseCase(userRepository)

  @Provides
  fun providesGetUserSettingsUseCase(userRepository: UserRepositoryInterface) =
    GetUserSettingsUseCase(userRepository)

  @Provides
  fun providesSyncSurveyUseCase(
    loiRepository: LocationOfInterestRepositoryInterface,
    surveyRepository: SurveyRepositoryInterface,
  ) = SyncSurveyUseCase(loiRepository, surveyRepository)

  @Provides
  fun providesSubmitDataUseCase(
    loiRepository: LocationOfInterestRepositoryInterface,
    submissionRepository: SubmissionRepositoryInterface,
  ) = SubmitDataUseCase(loiRepository, submissionRepository)

  @Provides
  fun providesGetSurveyListItemUseCase(surveyRepository: SurveyRepositoryInterface) =
    GetSurveyListItemUseCase(surveyRepository)
}
