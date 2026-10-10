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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroundFirestoreTest {
  private val firestore: FirebaseFirestore = mock()
  private val groundFirestore = GroundFirestore(firestore)

  @Test
  fun `surveys points at the surveys collection`() {
    val surveys = collectionAt("surveys")
    whenever(firestore.collection("surveys")).thenReturn(surveys)

    assertThat(groundFirestore.surveys().toString()).isEqualTo("surveys")
  }

  @Test
  fun `termsOfService points at the config collection`() {
    val config = collectionAt("config")
    whenever(firestore.collection("config")).thenReturn(config)

    assertThat(groundFirestore.termsOfService().toString()).isEqualTo("config")
  }

  @Test
  fun `terms resolves the tos document within config`() {
    val config = collectionAt("config")
    val tos: DocumentReference = mock()
    whenever(tos.path).thenReturn("config/tos")
    whenever(config.document("tos")).thenReturn(tos)
    whenever(firestore.collection("config")).thenReturn(config)

    assertThat(groundFirestore.termsOfService().terms().toString()).isEqualTo("config/tos")
  }

  @Test
  fun `batch delegates to the underlying database`() {
    val batch: WriteBatch = mock()
    whenever(firestore.batch()).thenReturn(batch)

    assertThat(groundFirestore.batch()).isSameInstanceAs(batch)
  }

  private fun collectionAt(path: String): CollectionReference {
    val reference: CollectionReference = mock()
    whenever(reference.path).thenReturn(path)
    return reference
  }
}
