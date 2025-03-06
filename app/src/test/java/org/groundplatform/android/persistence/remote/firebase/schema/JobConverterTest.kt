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

package org.groundplatform.android.persistence.remote.firebase.schema

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import org.groundplatform.android.FakeData
import org.groundplatform.android.model.job.Job as JobModel
import org.groundplatform.android.persistence.remote.firebase.protobuf.toFirestoreMap
import org.groundplatform.android.proto.Job
import org.groundplatform.android.proto.Task
import org.groundplatform.android.proto.job
import org.groundplatform.android.proto.style
import org.groundplatform.android.proto.task
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class JobConverterTest {

  @Test
  fun `Converts to Job from Job proto`() {
    with(FakeData) {
      val jobProto = job {
        id = JOB.id
        name = JOB.name.orEmpty()
        style = style { color = JOB.style?.color.orEmpty() }
      }
      val snapshot = createDocumentSnapshot(jobProto)
      assertThat(JobConverter.toJob(snapshot)).isEqualTo(JOB)
    }
  }

  @Test
  fun `Detects MIXED Job strategy from nested task proto`() {
    with(FakeData) {
      val jobProto = job {
        id = JOB.id
        name = JOB.name.orEmpty()
        style = style { color = JOB.style?.color.orEmpty() }
        tasks.addAll(listOf(task { level = Task.DataCollectionLevel.LOI_METADATA }))
      }
      val snapshot = createDocumentSnapshot(jobProto)
      assertThat(JobConverter.toJob(snapshot).strategy)
        .isEqualTo(JobModel.DataCollectionStrategy.MIXED)
    }
  }

  private fun createDocumentSnapshot(jobProto: Job): DocumentSnapshot {
    val snapshot = mock(DocumentSnapshot::class.java)
    whenever(snapshot.id).thenReturn(FakeData.JOB.id)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(jobProto.toFirestoreMap())
    return snapshot
  }
}
