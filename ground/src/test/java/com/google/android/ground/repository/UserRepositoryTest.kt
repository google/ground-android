/*
 * Copyright 2021 Google LLC
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

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.room.LocalDataStoreException
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.persistence.local.LocalDataStoreHelper
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class UserRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  @Inject lateinit var localUserStore: LocalUserStore

  @Inject lateinit var localDataStoreHelper: LocalDataStoreHelper

  @Inject lateinit var localValueStore: LocalValueStore

  @Inject lateinit var userRepository: UserRepository

  @Test
  fun `currentUser returns current user`() {
    fakeAuthenticationManager.setUser(FakeData.USER)
    assertThat(userRepository.currentUser).isEqualTo(FakeData.USER)
  }

  @Test
  fun `saveUserDetails() updates local user profile`() = runWithTestDispatcher {
    assertFailsWith<LocalDataStoreException> { localUserStore.getUser(FakeData.USER.id) }
    fakeAuthenticationManager.setUser(FakeData.USER)

    userRepository.saveUserDetails()

    assertThat(localUserStore.getUser(FakeData.USER.id)).isEqualTo(FakeData.USER)
  }

  @Test
  fun `clearUserPreferences() returns empty lastActiveSurveyId`() {
    localValueStore.lastActiveSurveyId = "foo"

    userRepository.clearUserPreferences()

    assertThat(localValueStore.lastActiveSurveyId).isEmpty()
  }
}
