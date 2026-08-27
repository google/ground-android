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
import org.groundplatform.domain.repository.MapStateRepositoryInterface
import org.groundplatform.domain.repository.OfflineAreaRepositoryInterface
import org.groundplatform.domain.repository.SubmissionRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.domain.system.NetworkManagerInterface
import org.groundplatform.domain.usecases.GetLoiReportUseCase
import org.groundplatform.domain.usecases.submission.SubmitDataUseCase
import org.groundplatform.domain.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.domain.usecases.survey.GetDataSharingTermsUseCase
import org.groundplatform.domain.usecases.survey.GetSurveyListItemUseCase
import org.groundplatform.domain.usecases.survey.ListAvailableSurveysUseCase
import org.groundplatform.domain.usecases.survey.MakeSurveyAvailableOfflineUseCase
import org.groundplatform.domain.usecases.survey.ReactivateLastSurveyUseCase
import org.groundplatform.domain.usecases.survey.RemoveOfflineSurveyUseCase
import org.groundplatform.domain.usecases.survey.SyncSurveyUseCase
import org.groundplatform.domain.usecases.user.ClearUserSessionUseCase
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

  @Provides
  fun providesClearUserSessionUseCase(
    offlineAreaRepository: OfflineAreaRepositoryInterface,
    surveyRepository: SurveyRepositoryInterface,
    userRepository: UserRepositoryInterface,
  ) = ClearUserSessionUseCase(offlineAreaRepository, surveyRepository, userRepository)

  @Provides
  fun providesRemoveOfflineSurveyUseCase(
    surveyRepository: SurveyRepositoryInterface,
    mapStateRepository: MapStateRepositoryInterface,
  ) = RemoveOfflineSurveyUseCase(surveyRepository, mapStateRepository)

  @Provides
  fun providesGetDataSharingTermsUseCase(surveyRepository: SurveyRepositoryInterface) =
    GetDataSharingTermsUseCase(surveyRepository)

  @Provides
  fun providesMakeSurveyAvailableOfflineUseCase(
    surveyRepository: SurveyRepositoryInterface,
    syncSurvey: SyncSurveyUseCase,
  ) = MakeSurveyAvailableOfflineUseCase(surveyRepository, syncSurvey)

  @Provides
  fun providesActivateSurveyUseCase(
    makeSurveyAvailableOffline: MakeSurveyAvailableOfflineUseCase,
    surveyRepository: SurveyRepositoryInterface,
  ) = ActivateSurveyUseCase(makeSurveyAvailableOffline, surveyRepository)

  @Provides
  fun providesReactivateLastSurveyUseCase(
    activateSurvey: ActivateSurveyUseCase,
    surveyRepository: SurveyRepositoryInterface,
  ) = ReactivateLastSurveyUseCase(activateSurvey, surveyRepository)

  @Provides
  fun providesListAvailableSurveysUseCase(
    networkManager: NetworkManagerInterface,
    surveyRepository: SurveyRepositoryInterface,
    userRepository: UserRepositoryInterface,
  ) = ListAvailableSurveysUseCase(networkManager, surveyRepository, userRepository)
}
