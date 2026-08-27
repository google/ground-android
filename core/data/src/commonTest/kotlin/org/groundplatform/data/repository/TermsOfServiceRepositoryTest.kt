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
package org.groundplatform.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.data.DataStoreException
import org.groundplatform.data.FakeLocalValueStore
import org.groundplatform.data.FakeRemoteDataStore
import org.groundplatform.domain.model.TermsOfService
import org.groundplatform.domain.system.NetworkStatus
import org.groundplatform.testing.FakeNetworkManager

class TermsOfServiceRepositoryTest {
  private val fakeNetworkManager = FakeNetworkManager(NetworkStatus.AVAILABLE)
  private val fakeRemoteDataStore = FakeRemoteDataStore()
  private val fakeLocalValueStore = FakeLocalValueStore()
  private val repository =
    TermsOfServiceRepository(fakeNetworkManager, fakeRemoteDataStore, fakeLocalValueStore)

  private val testTermsOfService = TermsOfService("tos_1", "Test Terms of Service")

  @Test
  fun getTermsOfService_success() = runTest {
    fakeRemoteDataStore.termsOfServiceResult = Result.success(testTermsOfService)

    assertEquals(testTermsOfService, repository.getTermsOfService())
  }

  @Test
  fun getTermsOfService_whenMissing_returnsNull() = runTest {
    fakeRemoteDataStore.termsOfServiceResult = Result.success(null)

    assertNull(repository.getTermsOfService())
  }

  @Test
  fun getTermsOfService_whenOffline_throwsDataStoreException() = runTest {
    fakeNetworkManager.networkStatusStateFlow.value = NetworkStatus.UNAVAILABLE

    assertFailsWith<DataStoreException> { repository.getTermsOfService() }
  }

  @Test
  fun getTermsOfService_whenRequestFails_throwsError() = runTest {
    fakeRemoteDataStore.termsOfServiceResult =
      Result.failure<TermsOfService?>(RuntimeException("Network error"))

    assertFailsWith<RuntimeException> { repository.getTermsOfService() }
  }

  @Test
  fun termsOfServiceAccepted_roundTrip() {
    repository.isTermsOfServiceAccepted = true
    assertTrue(repository.isTermsOfServiceAccepted)

    repository.isTermsOfServiceAccepted = false
    assertFalse(repository.isTermsOfServiceAccepted)
  }
}
