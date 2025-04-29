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
package org.groundplatform.android.persistence.remote.firebase

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.toListItem
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.remote.FakeRemoteDataStore
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.NetworkStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class FirestoreDataStoreTest : BaseHiltTest() {

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @BindValue @Mock lateinit var networkManager: NetworkManager
  @Inject lateinit var localSurveyStore: LocalSurveyStore

  @Before
  override fun setUp() {
    super.setUp()
    setupSurveys()
  }

  private fun setupSurveys() = runWithTestDispatcher {
    fakeRemoteDataStore.surveys = listOf(SURVEY_1, SURVEY_2)
    fakeRemoteDataStore.publicSurveys = listOf(PUBLIC_SURVEY_A, PUBLIC_SURVEY_B)
  }

  @Test
  fun `getRestrictedSurveyList emits mapped list`() = runWithTestDispatcher {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))
    val resultFlow = fakeRemoteDataStore.getRestrictedSurveyList(FakeData.USER)

    assertThat(resultFlow.first())
      .isEqualTo(
        listOf(
          SURVEY_1.toListItem(availableOffline = false),
          SURVEY_2.toListItem(availableOffline = false),
        )
      )
  }

  @Test
  fun `getPublicSurveyList emits mapped list`() = runWithTestDispatcher {
    whenever(networkManager.networkStatusFlow).thenReturn(flowOf(NetworkStatus.AVAILABLE))
    val resultFlow = fakeRemoteDataStore.getPublicSurveyList()

    assertThat(resultFlow.first())
      .isEqualTo(
        listOf(
          PUBLIC_SURVEY_A.toListItem(availableOffline = false),
          PUBLIC_SURVEY_B.toListItem(availableOffline = false),
        )
      )
  }

  companion object {
    private val SURVEY_1 =
      Survey(id = "1", title = "Survey 1", description = "", jobMap = emptyMap())
    private val SURVEY_2 =
      Survey(id = "2", title = "Survey 2", description = "", jobMap = emptyMap())

    private val PUBLIC_SURVEY_A =
      Survey(id = "A", title = "Public Survey 1", description = "", jobMap = emptyMap())
    private val PUBLIC_SURVEY_B =
      Survey(id = "B", title = "Public Survey 2", description = "", jobMap = emptyMap())
  }
}
