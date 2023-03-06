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
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.persistence.local.LocalDataStoreHelper
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class UserRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  @Inject lateinit var localUserStore: LocalUserStore

  @Inject lateinit var localDataStoreHelper: LocalDataStoreHelper

  @Inject lateinit var localValueStore: LocalValueStore

  @Inject lateinit var userRepository: UserRepository

  @Test
  fun testGetCurrentUser() {
    fakeAuthenticationManager.setUser(FakeData.USER)
    assertThat(userRepository.currentUser).isEqualTo(FakeData.USER)
  }

  @Test
  fun testSaveUser() {
    localUserStore
      .getUser(FakeData.USER.id)
      .test()
      .assertFailure(NoSuchElementException::class.java)
    userRepository.saveUser(FakeData.USER).test().assertComplete()
    localUserStore.getUser(FakeData.USER.id).test().assertResult(FakeData.USER)
  }

  @Test
  fun testClearUserPreferences_returnsEmptyLastActiveSurvey() {
    localValueStore.lastActiveSurveyId = "foo"
    userRepository.clearUserPreferences()
    assertThat(localValueStore.lastActiveSurveyId).isEmpty()
  }
}
