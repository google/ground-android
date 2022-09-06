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

import com.google.android.ground.model.Constants
import com.google.android.ground.model.Role
import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.auth.AuthenticationManager
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

/**
 * Coordinates persistence of [User] instance in local data store. For more details on this pattern
 * and overall architecture, see * https://developer.android.com/jetpack/docs/guide.
 */
class UserRepository
@Inject
constructor(
  private val authenticationManager: AuthenticationManager,
  private val localDataStore: LocalDataStore,
  private val localValueStore: LocalValueStore,
  private val schedulers: Schedulers,
  private val surveyRepository: SurveyRepository
) {

  val currentUser: User
    get() = authenticationManager.currentUser

  fun getUserRole(surveyId: String): Role {
    val survey = surveyRepository.getSurvey(surveyId).blockingGet()
    val value = survey.acl[currentUser.email]
    return toRole(value)
  }

  private fun toRole(roleString: String?): Role =
    when (roleString) {
      Constants.OWNER -> Role.OWNER
      Constants.SURVEY_ORGANIZER -> Role.SURVEY_ORGANIZER
      Constants.DATA_COLLECTOR -> Role.DATA_COLLECTOR
      else -> Role.UNKNOWN
    }

  fun saveUser(user: User): @Cold Completable =
    localDataStore.insertOrUpdateUser(user).observeOn(schedulers.io())

  fun getUser(userId: String): @Cold Single<User> = localDataStore.getUser(userId)

  /** Clears all user-specific preferences and settings. */
  fun clearUserPreferences() = localValueStore.clear()
}
