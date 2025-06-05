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
package org.groundplatform.android.persistence.local

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.model.User
import org.groundplatform.android.persistence.local.stores.LocalUserStore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalUserStoreTest : BaseHiltTest() {
  @Inject lateinit var localUserStore: LocalUserStore

  @Test
  fun testInsertAndGetUser() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    assertThat(localUserStore.getUser(FakeData.USER_ID)).isEqualTo(TEST_USER)
  }

  companion object {
    private val TEST_USER = User(FakeData.USER_ID, "user@gmail.com", "user 1")
  }
}
