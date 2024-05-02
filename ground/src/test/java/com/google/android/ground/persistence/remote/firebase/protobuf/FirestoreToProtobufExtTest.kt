/*
 * Copyright 2024 Google LLC
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

package com.google.android.ground.persistence.remote.firebase.protobuf

import com.google.android.ground.persistence.remote.firebase.newDocumentSnapshot
import com.google.android.ground.proto.Survey
import com.google.android.ground.proto.job
import com.google.android.ground.proto.survey
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import org.junit.Test

class FirestoreToProtobufExtTest {
  @Test
  fun `toMessage() converts id`() {
    val doc = newDocumentSnapshot(id = "12345")
    assertThat(doc.toMessage()).isEqualTo(survey { id = "12345" })
  }

  @Test
  fun `toMessage() converts strings`() {
    val doc = newDocumentSnapshot(data = mapOf("title" to "Test survey"))
    assertThat(doc.toMessage()).isEqualTo(survey { title = "Test survey" })
  }

  @Test
  fun `toMessage() converts message maps`() {
    val doc =
      newDocumentSnapshot(data = mapOf("jobs" to mapOf("job123" to mapOf("name" to "Test job"))))
    assertThat(doc.toMessage()).isEqualTo(survey { jobs["job123"] = job { name = "Test job" } })
  }

  private fun DocumentSnapshot.toMessage(): Survey = toMessage(Survey::class)
}
