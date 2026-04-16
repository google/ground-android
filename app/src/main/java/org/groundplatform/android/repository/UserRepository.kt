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
import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.local.stores.LocalUserStore
import org.groundplatform.android.data.remote.RemoteDataStore
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.domain.model.Role
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.auth.SignInState
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.model.settings.UserSettings
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
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
  private val surveyRepository: SurveyRepositoryInterface,
  private val remoteDataStore: RemoteDataStore,
) : UserRepositoryInterface {

  override fun getSignInState(): Flow<SignInState> = authenticationManager.signInState

  override fun init() = authenticationManager.init()

  override fun signIn() = authenticationManager.signIn()

  override fun signOut() = authenticationManager.signOut()

  override suspend fun getAuthenticatedUser(): User = authenticationManager.getAuthenticatedUser()

  override suspend fun saveUserDetails(user: User) {
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

  override suspend fun getUser(userId: String): User = localUserStore.getUser(userId)

  override fun clearUserPreferences() = localValueStore.clear()

  override suspend fun canUserSubmitData(): Boolean {
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

  override suspend fun canDeleteLoi(loi: LocationOfInterest): Boolean {
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

  override fun getUserSettings(): UserSettings =
    with(localValueStore) {
      UserSettings(
        language = selectedLanguage,
        measurementUnits = MeasurementUnits.valueOf(selectedLengthUnit),
        shouldUploadPhotosOnWifiOnly = shouldUploadMediaOverUnmeteredConnectionOnly,
      )
    }

  override fun setUserSettings(userSettings: UserSettings) {
    with(localValueStore) {
      selectedLanguage = userSettings.language
      selectedLengthUnit = userSettings.measurementUnits.name
      shouldUploadMediaOverUnmeteredConnectionOnly = userSettings.shouldUploadPhotosOnWifiOnly
    }
  }
}
