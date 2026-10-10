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

package org.groundplatform.android.data.remote.firebase.schema

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.data.remote.firebase.canceledTask
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JobCollectionReferenceTest {
  private val collectionReference: CollectionReference = mock()
  private val jobCollectionReference = JobCollectionReference(collectionReference)

  @Test
  fun `get returns no jobs when the collection is empty`() = runTest {
    val snapshot: QuerySnapshot = mock()
    whenever(snapshot.iterator()).thenReturn(mutableListOf<QueryDocumentSnapshot>().iterator())
    whenever(collectionReference.get()).thenReturn(Tasks.forResult(snapshot))

    assertThat(jobCollectionReference.get()).isEmpty()
  }

  @Test
  fun `get returns no jobs when the fetch is cancelled`() = runTest {
    val cancelled = canceledTask<QuerySnapshot>()
    whenever(collectionReference.get()).thenReturn(cancelled)

    // Cancellation is swallowed so a survey sync aborted mid-flight doesn't surface as an error.
    assertThat(jobCollectionReference.get()).isEmpty()
  }
}
