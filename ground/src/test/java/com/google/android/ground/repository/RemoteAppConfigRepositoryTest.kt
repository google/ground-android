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
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class RemoteAppConfigRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @Inject lateinit var remoteAppConfigRepository: RemoteAppConfigRepository

  @Test
  fun testGetTermsOfService() = runBlocking {
    fakeRemoteDataStore.remoteAppConfig = Result.success(FakeData.TERMS_OF_SERVICE)
    assertThat(remoteAppConfigRepository.getRemoteAppConfig()).isEqualTo(FakeData.TERMS_OF_SERVICE)
  }

  @Test
  fun testGetTermsOfService_whenMissing_returnsNull() = runBlocking {
    assertThat(remoteAppConfigRepository.getRemoteAppConfig()).isNull()
  }

  @Test
  fun testGetTermsOfService_whenRequestFails_throwsError() {
    fakeRemoteDataStore.remoteAppConfig =
      Result.failure(
        FirebaseFirestoreException("user error", FirebaseFirestoreException.Code.ABORTED)
      )
    assertThrows(FirebaseFirestoreException::class.java) {
      runBlocking { remoteAppConfigRepository.getRemoteAppConfig() }
    }
  }

  @Test
  fun testTermsOfServiceAccepted() {
    remoteAppConfigRepository.isTermsOfServiceAccepted = true
    assertThat(remoteAppConfigRepository.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun testTermsOfServiceNotAccepted() {
    assertThat(remoteAppConfigRepository.isTermsOfServiceAccepted).isFalse()
  }
}
