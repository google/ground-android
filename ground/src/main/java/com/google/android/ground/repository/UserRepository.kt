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
package com.google.android.ground.repository

import com.google.android.ground.model.Role
import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.system.NetworkManager
import com.google.android.ground.system.auth.AuthenticationManager
import javax.inject.Inject
import javax.inject.Singleton
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
    val user = getAuthenticatedUser()
    return try {
      surveyRepository.activeSurvey?.getRole(user.email) != Role.VIEWER
    } catch (e: IllegalStateException) {
      Timber.e(e, "Error getting role for user $user")
      false
    }
  }
}
