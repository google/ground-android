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

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import org.groundplatform.android.FakeData
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.model.mutation.SubmissionMutation
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubmissionDocumentReferenceTest {
  private val documentReference: DocumentReference = mock()
  private val batch: WriteBatch = mock()
  private val submissionDocumentReference = SubmissionDocumentReference(documentReference)

  @Test
  fun `addMutationToBatch merges the submission on CREATE`() {
    submissionDocumentReference.addMutationToBatch(
      newSubmissionMutation(Mutation.Type.CREATE),
      FakeData.USER,
      batch,
    )

    verify(batch).set(eq(documentReference), any<Map<String, Any>>(), eq(SetOptions.merge()))
    verify(batch, never()).delete(any())
  }

  @Test
  fun `addMutationToBatch merges the submission on UPDATE`() {
    submissionDocumentReference.addMutationToBatch(
      newSubmissionMutation(Mutation.Type.UPDATE),
      FakeData.USER,
      batch,
    )

    verify(batch).set(eq(documentReference), any<Map<String, Any>>(), eq(SetOptions.merge()))
    verify(batch, never()).delete(any())
  }

  @Test
  fun `addMutationToBatch deletes the document on DELETE`() {
    submissionDocumentReference.addMutationToBatch(
      newSubmissionMutation(Mutation.Type.DELETE),
      FakeData.USER,
      batch,
    )

    verify(batch).delete(documentReference)
    verify(batch, never()).set(any<DocumentReference>(), any<Map<String, Any>>(), any())
  }

  @Test
  fun `addMutationToBatch rejects an unknown mutation type`() {
    val mutation = newSubmissionMutation(Mutation.Type.UNKNOWN)

    assertThrows(IllegalArgumentException::class.java) {
      submissionDocumentReference.addMutationToBatch(mutation, FakeData.USER, batch)
    }
  }

  private fun newSubmissionMutation(type: Mutation.Type) =
    SubmissionMutation(
      id = 1L,
      submissionId = "submissionId",
      surveyId = "surveyId",
      locationOfInterestId = "loiId",
      userId = FakeData.USER.id,
      clientTimestamp = 987654321L,
      job = FakeData.JOB,
      collectionId = "collectionId",
      type = type,
      syncStatus = Mutation.SyncStatus.PENDING,
      deltas = listOf(),
    )
}
