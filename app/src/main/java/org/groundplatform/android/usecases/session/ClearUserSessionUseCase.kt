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
package org.groundplatform.android.usecases.session

import javax.inject.Inject
import org.groundplatform.android.data.local.room.LocalDatabase
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.UserRepository

/**
 * Use case to clear the user's session data and preferences. This includes removing all offline
 * areas, clearing the active survey, resetting user preferences, and clearing all local database
 * tables.
 *
 * Warning: This operation is destructive and will remove all locally stored data. It is primarily
 * intended for use during sign-out or when switching users.
 */
class ClearUserSessionUseCase
@Inject
constructor(
  private val localDatabase: LocalDatabase,
  private val offlineAreaRepository: OfflineAreaRepository,
  private val surveyRepository: SurveyRepository,
  private val userRepository: UserRepository,
) {

  suspend operator fun invoke() {
    offlineAreaRepository.removeAllOfflineAreas()
    surveyRepository.clearActiveSurvey()
    userRepository.clearUserPreferences()

    // TODO: Once multi-user login is supported, avoid clearing local db data. This is
    //  currently being done to prevent one user's data to be submitted as another user after
    //  re-login.
    // Issue URL: https://github.com/google/ground-android/issues/1691
    localDatabase.clearAllTables()
  }
}
