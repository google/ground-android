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

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.CollectionReference
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
class SurveyDocumentReferenceTest {
  private val documentReference: DocumentReference = mock()
  private val surveyDocumentReference = SurveyDocumentReference(documentReference)

  @Test
  fun `lois points at the survey's lois subcollection`() {
    val lois = collectionAt("surveys/s1/lois")
    whenever(documentReference.collection("lois")).thenReturn(lois)

    assertThat(surveyDocumentReference.lois().toString()).isEqualTo("surveys/s1/lois")
  }

  @Test
  fun `submissions points at the survey's submissions subcollection`() {
    val submissions = collectionAt("surveys/s1/submissions")
    whenever(documentReference.collection("submissions")).thenReturn(submissions)

    assertThat(surveyDocumentReference.submissions().toString()).isEqualTo("surveys/s1/submissions")
  }

  @Test
  fun `get returns null when the fetch is cancelled`() = runTest {
    val cancelled = canceledTask<DocumentSnapshot>()
    whenever(documentReference.get()).thenReturn(cancelled)

    assertThat(surveyDocumentReference.get()).isNull()
  }

  private fun collectionAt(path: String): CollectionReference {
    val reference: CollectionReference = mock()
    whenever(reference.path).thenReturn(path)
    return reference
  }
}
