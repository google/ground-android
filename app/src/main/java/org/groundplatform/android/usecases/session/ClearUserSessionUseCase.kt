package org.groundplatform.android.usecases.session

import javax.inject.Inject
import org.groundplatform.android.persistence.local.room.LocalDatabase
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
