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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.data.remote.firebase.canceledTask
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TermsOfServiceDocumentReferenceTest {
  private val documentReference: DocumentReference = mock()
  private val termsOfServiceDocumentReference = TermsOfServiceDocumentReference(documentReference)

  @Test
  fun `get returns the terms held by the document`() = runTest {
    val snapshot: DocumentSnapshot = mock()
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.id).thenReturn("tos")
    whenever(snapshot.toObject(TermsOfServiceDocument::class.java))
      .thenReturn(TermsOfServiceDocument("Terms text"))
    whenever(documentReference.get()).thenReturn(Tasks.forResult(snapshot))

    val terms = termsOfServiceDocumentReference.get()

    assertThat(terms?.id).isEqualTo("tos")
    assertThat(terms?.text).isEqualTo("Terms text")
  }

  @Test
  fun `get returns null when the document is absent`() = runTest {
    val snapshot: DocumentSnapshot = mock()
    whenever(snapshot.exists()).thenReturn(false)
    whenever(documentReference.get()).thenReturn(Tasks.forResult(snapshot))

    assertThat(termsOfServiceDocumentReference.get()).isNull()
  }

  @Test
  fun `get returns null when the fetch is cancelled`() = runTest {
    val cancelled = canceledTask<DocumentSnapshot>()
    whenever(documentReference.get()).thenReturn(cancelled)

    assertThat(termsOfServiceDocumentReference.get()).isNull()
  }

  @Test
  fun `terms returns a reference to the same document`() {
    whenever(documentReference.path).thenReturn("config/tos")

    assertThat(termsOfServiceDocumentReference.terms().toString()).isEqualTo("config/tos")
  }
}
