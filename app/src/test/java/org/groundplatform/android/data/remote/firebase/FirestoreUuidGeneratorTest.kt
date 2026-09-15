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

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirestoreUuidGeneratorTest {

  @Test
  fun `generateUuid returns the id Firestore reserved for a new document`() = runTest {
    val document: DocumentReference = mock()
    whenever(document.id).thenReturn("reserved-id")
    val collection: CollectionReference = mock()
    whenever(collection.document()).thenReturn(document)
    val firestore: FirebaseFirestore = mock()
    whenever(firestore.collection(FirestoreUuidGenerator.ID_COLLECTION)).thenReturn(collection)
    val provider: FirebaseFirestoreProvider = mock { on { get() } doReturn firestore }

    // The document is never written; Firestore only allocates the id locally.
    assertThat(FirestoreUuidGenerator(provider).generateUuid()).isEqualTo("reserved-id")
  }
}
