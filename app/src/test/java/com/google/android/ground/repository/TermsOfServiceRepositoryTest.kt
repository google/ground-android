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
import com.google.android.ground.FakeData
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.FakeRemoteDataStore
import com.google.android.ground.system.NetworkManager
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TermsOfServiceRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var termsOfServiceRepository: TermsOfServiceRepository

  @BindValue @Mock lateinit var mockNetworkManager: NetworkManager

  @Test
  fun testGetTermsOfService() = runBlocking {
    whenever(mockNetworkManager.isNetworkConnected()).thenReturn(true)
    fakeRemoteDataStore.termsOfService = Result.success(FakeData.TERMS_OF_SERVICE)

    assertThat(termsOfServiceRepository.getTermsOfService()).isEqualTo(FakeData.TERMS_OF_SERVICE)
  }

  @Test
  fun testGetTermsOfService_whenMissing_returnsNull() = runBlocking {
    whenever(mockNetworkManager.isNetworkConnected()).thenReturn(true)

    assertThat(termsOfServiceRepository.getTermsOfService()).isNull()
  }

  @Test
  fun testGetTermsOfService_whenRequestFails_throwsError() {
    whenever(mockNetworkManager.isNetworkConnected()).thenReturn(true)
    fakeRemoteDataStore.termsOfService =
      Result.failure(
        FirebaseFirestoreException("user error", FirebaseFirestoreException.Code.ABORTED)
      )

    assertThrows(FirebaseFirestoreException::class.java) {
      runBlocking { termsOfServiceRepository.getTermsOfService() }
    }
  }

  @Test
  fun testGetTermsOfService_whenOffline_throwsError() {
    whenever(mockNetworkManager.isNetworkConnected()).thenReturn(false)
    fakeRemoteDataStore.termsOfService =
      Result.failure(
        FirebaseFirestoreException("user error", FirebaseFirestoreException.Code.ABORTED)
      )

    assertThrows(DataStoreException::class.java) {
      runBlocking { termsOfServiceRepository.getTermsOfService() }
    }
  }

  @Test
  fun testGetTermsOfService_whenServiceUnavailable_throwsError() {
    whenever(mockNetworkManager.isNetworkConnected()).thenReturn(true)
    fakeRemoteDataStore.termsOfService =
      Result.failure(
        FirebaseFirestoreException("device offline", FirebaseFirestoreException.Code.UNAVAILABLE)
      )

    assertThrows(FirebaseFirestoreException::class.java) {
      runBlocking { termsOfServiceRepository.getTermsOfService() }
    }
  }

  @Test
  fun testTermsOfServiceAccepted() {
    termsOfServiceRepository.isTermsOfServiceAccepted = true
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun testTermsOfServiceNotAccepted() {
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isFalse()
  }
}
