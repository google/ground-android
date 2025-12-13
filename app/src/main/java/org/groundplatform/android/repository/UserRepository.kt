/*
 * Copyright 2019 Google LLC
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
package org.groundplatform.android.repository

import javax.inject.Inject
import javax.inject.Singleton
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.local.stores.LocalUserStore
import org.groundplatform.android.data.remote.RemoteDataStore
import org.groundplatform.android.model.Role
import org.groundplatform.android.model.User
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.proto.Survey
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.auth.AuthenticationManager
import timber.log.Timber

/**
 * Coordinates persistence of [User] instance in local data store. For more details on this pattern
 * and overall architecture, see https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
class UserRepository
@Inject
constructor(
  private val authenticationManager: AuthenticationManager,
  private val localValueStore: LocalValueStore,
  private val localUserStore: LocalUserStore,
  private val networkManager: NetworkManager,
  private val surveyRepository: SurveyRepository,
  private val remoteDataStore: RemoteDataStore,
) {

  fun getSignInState() = authenticationManager.signInState

  fun init() = authenticationManager.init()

  fun signIn() = authenticationManager.signIn()

  fun signOut() = authenticationManager.signOut()

  suspend fun getAuthenticatedUser() = authenticationManager.getAuthenticatedUser()

  /** Stores the given user to the local and remote dbs. */
  suspend fun saveUserDetails(user: User) {
    localUserStore.insertOrUpdateUser(user)
    updateRemoteUserInfo(user)
  }

  /** Attempts to refresh current user's profile in remote database if network is available. */
  private suspend fun updateRemoteUserInfo(user: User) {
    if (!networkManager.isNetworkConnected()) {
      Timber.d("Skipped refreshing user profile as device is offline.")
      return
    }
    if (!user.isAnonymous) {
      runCatching { remoteDataStore.refreshUserProfile() }
        .fold(
          { Timber.i("Profile refreshed") },
          { throwable -> Timber.e(throwable, "Failed to refresh profile") },
        )
    }
  }

  suspend fun getUser(userId: String): User = localUserStore.getUser(userId)

  /** Clears all user-specific preferences and settings. */
  fun clearUserPreferences() = localValueStore.clear()

  /**
   * Returns true if the currently logged in user has permissions to write data to the active
   * survey. If no survey is active at the moment, then it returns false.
   */
  suspend fun canUserSubmitData(): Boolean {
    if (
      surveyRepository.activeSurvey?.generalAccess == Survey.GeneralAccess.PUBLIC ||
        surveyRepository.activeSurvey?.generalAccess == Survey.GeneralAccess.UNLISTED
    ) {
      return true
    }

    val user = getAuthenticatedUser()
    return try {
      surveyRepository.activeSurvey?.getRole(user.email) != Role.VIEWER
    } catch (e: IllegalStateException) {
      Timber.e(e, "Error getting role for user $user")
      false
    }
  }

  /**
   * Returns true if the currently logged in user can delete the given LOI. This is allowed if:
   * - The user is a survey organizer, OR
   * - The user created the LOI (data collector who created their own site)
   */
  suspend fun canDeleteLoi(loi: LocationOfInterest): Boolean {
    if (loi.isPredefined == true) return false

    val user = getAuthenticatedUser()
    val ownerId = loi.created.user.id
    if (ownerId == user.id) return true

    // Check if user is a survey organizer
    val isOrganizer =
      runCatching { surveyRepository.activeSurvey?.getRole(user.email) == Role.SURVEY_ORGANIZER }
        .getOrElse { false }

    return isOrganizer
  }

  fun getUserSettings(): UserSettings =
    with(localValueStore) {
      UserSettings(
        language = selectedLanguage,
        measurementUnits = MeasurementUnits.valueOf(selectedLengthUnit),
        shouldUploadPhotosOnWifiOnly = shouldUploadMediaOverUnmeteredConnectionOnly(),
      )
    }
}
