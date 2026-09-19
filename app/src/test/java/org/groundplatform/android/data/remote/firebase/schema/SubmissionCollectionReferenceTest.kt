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
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubmissionCollectionReferenceTest {
  private val collectionReference: CollectionReference = mock()
  private val submissionCollectionReference = SubmissionCollectionReference(collectionReference)

  @Test
  fun `submission resolves the document with the given id`() {
    val document: DocumentReference = mock()
    whenever(document.path).thenReturn("surveys/s1/submissions/sub1")
    whenever(collectionReference.document("sub1")).thenReturn(document)

    assertThat(submissionCollectionReference.submission("sub1").toString())
      .isEqualTo("surveys/s1/submissions/sub1")
  }

  @Test
  fun `toString reports the collection path`() {
    whenever(collectionReference.path).thenReturn("surveys/s1/submissions")

    assertThat(submissionCollectionReference.toString()).isEqualTo("surveys/s1/submissions")
  }
}
