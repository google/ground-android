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
package org.groundplatform.domain.repository

import kotlinx.coroutines.flow.Flow
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.auth.SignInState
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.settings.UserSettings

interface UserRepositoryInterface {
  fun getSignInState(): Flow<SignInState>

  fun init()

  fun signIn()

  fun signOut()

  suspend fun getAuthenticatedUser(): User

  /** Stores the given user to the local and remote dbs. */
  suspend fun saveUserDetails(user: User)

  suspend fun getUser(userId: String): User

  /** Clears all user-specific preferences and settings. */
  fun clearUserPreferences()

  /**
   * Returns true if the currently logged in user has permissions to write data to the active
   * survey. If no survey is active at the moment, then it returns false.
   */
  suspend fun canUserSubmitData(): Boolean

  /**
   * Returns true if the currently logged in user can delete the given LOI. This is allowed if:
   * - The user is a survey organizer, OR
   * - The user created the LOI (data collector who created their own site)
   */
  suspend fun canDeleteLoi(loi: LocationOfInterest): Boolean

  fun getUserSettings(): UserSettings

  fun setUserSettings(userSettings: UserSettings)
}
