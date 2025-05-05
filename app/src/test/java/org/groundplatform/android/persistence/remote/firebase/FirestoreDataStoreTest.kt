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

import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.USER_ID
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.User
import org.groundplatform.android.model.toListItem
import org.groundplatform.android.persistence.remote.firebase.schema.GroundFirestore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class FirestoreDataStoreTest : BaseHiltTest() {

  var firebaseFunctions: FirebaseFunctions = mock()
  lateinit var firebaseFirestoreProvider: FirebaseFirestoreProvider
  var firebaseFirestoreSettings: FirebaseFirestoreSettings = mock()

  lateinit var groundFirestore: GroundFirestore
  lateinit var firestoreDataStore: FirestoreDataStore

  @Before
  override fun setUp() = runBlocking {
    super.setUp()
    FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext)
    firebaseFirestoreProvider = FirebaseFirestoreProvider(firebaseFirestoreSettings)
    groundFirestore = GroundFirestore(firebaseFirestoreProvider.get())
    firestoreDataStore =
      FirestoreDataStore(firebaseFunctions, firebaseFirestoreProvider, testDispatcher)
  }

  @Test
  fun `getRestrictedSurveyList emits mapped list`() = runWithTestDispatcher {
    firestoreDataStore.getRestrictedSurveyList(User(USER_ID, "fakeEmail", "User")).test {
      assertThat(expectMostRecentItem())
        .isEqualTo(
          listOf(
            PUBLIC_SURVEY_A.toListItem(availableOffline = false),
            PUBLIC_SURVEY_B.toListItem(availableOffline = false),
          )
        )
    }
  }

  @Test
  fun `getPublicSurveyList emits mapped list`() = runWithTestDispatcher {
    firestoreDataStore.getPublicSurveyList().test {
      assertThat(expectMostRecentItem())
        .isEqualTo(
          listOf(
            PUBLIC_SURVEY_A.toListItem(availableOffline = false),
            PUBLIC_SURVEY_B.toListItem(availableOffline = false),
          )
        )
    }
  }

  companion object {
    private val PUBLIC_SURVEY_A =
      Survey(id = "A", title = "Public Survey 1", description = "", jobMap = emptyMap())
    private val PUBLIC_SURVEY_B =
      Survey(id = "B", title = "Public Survey 2", description = "", jobMap = emptyMap())
  }
}
