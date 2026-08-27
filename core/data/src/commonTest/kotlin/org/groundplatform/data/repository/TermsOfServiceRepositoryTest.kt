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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.groundplatform.data.DataStoreException
import org.groundplatform.data.FakeLocalValueStore
import org.groundplatform.data.stores.RemoteDataStore
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.model.TermsOfService
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.system.NetworkStatus
import org.groundplatform.testing.FakeNetworkManager

class TermsOfServiceRepositoryTest {
  private val fakeNetworkManager = FakeNetworkManager(NetworkStatus.AVAILABLE)
  private val fakeRemoteDataStore = TestRemoteDataStore()
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
    fakeRemoteDataStore.termsOfServiceResult = Result.failure(RuntimeException("Network error"))

    assertFailsWith<RuntimeException> { repository.getTermsOfService() }
  }

  @Test
  fun termsOfServiceAccepted_roundTrip() {
    repository.isTermsOfServiceAccepted = true
    assertTrue(repository.isTermsOfServiceAccepted)

    repository.isTermsOfServiceAccepted = false
    assertFalse(repository.isTermsOfServiceAccepted)
  }

  private class TestRemoteDataStore : RemoteDataStore {
    var termsOfServiceResult: Result<TermsOfService?> = Result.success(null)

    override suspend fun loadTermsOfService(): TermsOfService? = termsOfServiceResult.getOrThrow()

    override fun getRestrictedSurveyList(user: User): Flow<List<SurveyListItem>> = emptyFlow()

    override fun getPublicSurveyList(): Flow<List<SurveyListItem>> = emptyFlow()

    override suspend fun loadSurvey(surveyId: String): Survey? = null

    override fun loadPredefinedLois(survey: Survey): Flow<List<LocationOfInterest>> = emptyFlow()

    override fun loadUserLois(survey: Survey, ownerUserId: String): Flow<List<LocationOfInterest>> =
      emptyFlow()

    override fun loadSharedLois(survey: Survey): Flow<List<LocationOfInterest>> = emptyFlow()

    override suspend fun applyMutations(mutations: List<Mutation>, user: User) = Unit

    override suspend fun subscribeToSurveyUpdates(surveyId: String) = Unit

    override suspend fun unsubscribeFromSurveyUpdates(surveyId: String) = Unit

    override suspend fun refreshUserProfile() = Unit
  }
}
