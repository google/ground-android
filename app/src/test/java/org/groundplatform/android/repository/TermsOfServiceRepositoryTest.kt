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
package org.groundplatform.android.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.data.remote.DataStoreException
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.system.NetworkManager
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
  fun `get terms of service`() = runBlocking {
    whenever(mockNetworkManager.isNetworkConnected()).thenReturn(true)
    fakeRemoteDataStore.termsOfService = Result.success(FakeData.TERMS_OF_SERVICE)

    assertThat(termsOfServiceRepository.getTermsOfService()).isEqualTo(FakeData.TERMS_OF_SERVICE)
  }

  @Test
  fun `get terms of service when missing returns null`() = runBlocking {
    whenever(mockNetworkManager.isNetworkConnected()).thenReturn(true)

    assertThat(termsOfServiceRepository.getTermsOfService()).isNull()
  }

  @Test
  fun `get terms of service when request fails throws error`() {
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
  fun `get terms of service when offline throws error`() {
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
  fun `get terms of service when service unavailable throws error`() {
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
  fun `terms of service accepted`() {
    termsOfServiceRepository.isTermsOfServiceAccepted = true
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun `terms of service not accepted`() {
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isFalse()
  }
}
