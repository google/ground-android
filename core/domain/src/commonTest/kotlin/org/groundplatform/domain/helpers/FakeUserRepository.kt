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
package org.groundplatform.domain.helpers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.auth.SignInState
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.settings.UserSettings
import org.groundplatform.domain.repository.UserRepositoryInterface

internal class FakeUserRepository : UserRepositoryInterface {
  var currentUser: User = FakeData.newUser()
  var currentUserSettings: UserSettings = FakeData.newUserSettings()
  var signInState: SignInState = SignInState.SignedIn(currentUser)
  var canSubmitData = true
  var canDeleteLoi = true

  override fun getSignInState(): Flow<SignInState> = flowOf(signInState)

  override fun init() {
    /* Nothing to do */
  }

  override fun signIn() {
    currentUser = FakeData.newUser()
    signInState = SignInState.SignedIn(currentUser)
  }

  override fun signOut() {
    signInState = SignInState.SignedOut
  }

  override suspend fun getAuthenticatedUser(): User = currentUser

  override suspend fun saveUserDetails(user: User) {
    currentUser = user
  }

  override suspend fun getUser(userId: String): User = currentUser

  override fun clearUserPreferences() {
    /* Nothing to do */
  }

  override suspend fun canUserSubmitData(): Boolean = canSubmitData

  override suspend fun canDeleteLoi(loi: LocationOfInterest): Boolean = canDeleteLoi

  override fun getUserSettings(): UserSettings = currentUserSettings

  override fun setUserSettings(userSettings: UserSettings) {
    currentUserSettings = userSettings
  }
}
