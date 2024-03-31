/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.TermsOfService
import com.google.android.ground.persistence.remote.firebase.schema.TermsOfServiceConverter.toTerms
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class TermsOfServiceConverterTest {
  @Mock private lateinit var termsOfServiceDocumentSnapshot: DocumentSnapshot

  private lateinit var termsOfService: TermsOfService

  @Before
  fun setup() {
    termsOfService = TermsOfService(TEST_TERMS_ID, TEST_TERMS)
  }

  @Test
  fun testTermsOfService() {
    mockTermsOfServiceDocumentSnapshot(TermsOfServiceDocument(TEST_TERMS))
    assertThat(toTermsOfService()).isEqualTo(termsOfService)
  }

  /** Mock submission document snapshot to return the specified id and object representation. */
  private fun mockTermsOfServiceDocumentSnapshot(doc: TermsOfServiceDocument) {
    whenever(termsOfServiceDocumentSnapshot.id).thenReturn(TEST_TERMS_ID)
    whenever(termsOfServiceDocumentSnapshot.toObject(TermsOfServiceDocument::class.java))
      .thenReturn(doc)
    whenever(termsOfServiceDocumentSnapshot.exists()).thenReturn(true)
  }

  private fun toTermsOfService(): TermsOfService? = toTerms(termsOfServiceDocumentSnapshot)

  companion object {
    private const val TEST_TERMS = "TERMS"
    private const val TEST_TERMS_ID = "TERMS_ID"
  }
}
