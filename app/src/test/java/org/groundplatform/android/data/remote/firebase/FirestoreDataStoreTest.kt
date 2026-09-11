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
package org.groundplatform.android.data.remote.firebase

import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.data.remote.UpdateRequiredException
import org.groundplatform.domain.model.AppConfig
import org.groundplatform.domain.usecases.ShouldForceUpdateUseCase
import org.groundplatform.testing.FakeAppConfigRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirestoreDataStoreTest {

  private val firestoreProvider: FirebaseFirestoreProvider = mock()
  private val appConfigRepository =
    FakeAppConfigRepository().apply {
      config = AppConfig(minAppVersion = "2.0.0", forceUpdate = true)
    }
  private val dataStore =
    FirestoreDataStore(
      firebaseFunctions = mock(),
      firestoreProvider = firestoreProvider,
      shouldForceUpdate = ShouldForceUpdateUseCase(appConfigRepository, currentVersion = "1.0.0"),
      ioDispatcher = UnconfinedTestDispatcher(),
    )

  @Test
  fun `Refuses one-off reads when an app update is required`() = runTest {
    assertFailsWith<UpdateRequiredException> { dataStore.loadSurvey("surveyId") }
    verifyNoInteractions(firestoreProvider)
  }

  @Test
  fun `Refuses listeners when an app update is required`() = runTest {
    assertFailsWith<UpdateRequiredException> { dataStore.getPublicSurveyList().first() }
    verifyNoInteractions(firestoreProvider)
  }

  @Test
  fun `Refuses uploads when an app update is required`() = runTest {
    assertFailsWith<UpdateRequiredException> {
      dataStore.applyMutations(emptyList(), FakeData.USER)
    }
    verifyNoInteractions(firestoreProvider)
  }
}
