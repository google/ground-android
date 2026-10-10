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

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.functions.FirebaseFunctions
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.FakeData
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.mutation.LocationOfInterestMutation
import org.groundplatform.domain.model.mutation.Mutation
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirestoreDataStoreTest {
  private val batch: WriteBatch = mock()
  private val firestore: FirebaseFirestore = mock()
  private val functions: FirebaseFunctions = mock()

  private val dataStore by lazy {
    val provider: FirebaseFirestoreProvider = mock { on { get() } doReturn firestore }
    FirestoreDataStore(functions, provider, UnconfinedTestDispatcher())
  }

  @Test
  fun `applyMutations commits an empty batch without touching any document`() = runTest {
    whenever(firestore.batch()).thenReturn(batch)
    whenever(batch.commit()).thenReturn(Tasks.forResult(null))

    dataStore.applyMutations(listOf(), FakeData.USER)

    verify(batch).commit()
  }

  @Test
  fun `applyMutations rejects a mutation belonging to another user`() = runTest {
    whenever(firestore.batch()).thenReturn(batch)
    val otherUsersMutation = newLoiMutation(userId = "someone-else")

    // Guards against uploading one user's edits under another user's credentials.
    val error =
      assertFailsWith<IllegalStateException> {
        dataStore.applyMutations(listOf(otherUsersMutation), CURRENT_USER)
      }

    assertThat(error).hasMessageThat().contains("someone-else")
    verify(batch, never()).commit()
  }

  private fun newLoiMutation(userId: String) =
    LocationOfInterestMutation(
      jobId = "jobId",
      geometry = Point(Coordinates(88.0, -23.1)),
      id = 1L,
      locationOfInterestId = "loiId",
      type = Mutation.Type.CREATE,
      syncStatus = Mutation.SyncStatus.PENDING,
      userId = userId,
      surveyId = "surveyId",
      clientTimestamp = 987654321L,
      collectionId = "collectionId",
    )

  private companion object {
    val CURRENT_USER = User("current-user", "current@example.com", "Current")
  }
}
