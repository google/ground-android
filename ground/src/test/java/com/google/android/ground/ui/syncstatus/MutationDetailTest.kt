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
package com.google.android.ground.ui.syncstatus

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MutationDetailTest {

  @Test
  fun `test getUser() value should be same what set value`() {
    assertThat(mutationDetail.user).isEqualTo("Jane Doe")
  }

  @Test
  fun `test getMutation() value should be same what set value`() {
    assertThat(mutationDetail.mutation.type).isEqualTo(Mutation.Type.UNKNOWN)
  }

  @Test
  fun `test getLoiLabel() value should be same what set value`() {
    assertThat(mutationDetail.loiLabel).isEqualTo("Map the farms")
  }

  @Test
  fun `test getLoiSubtitle() value should be same what set value`() {
    assertThat(mutationDetail.loiSubtitle).isEqualTo("IDX21311")
  }

  companion object {
    private val mutationDetail =
      MutationDetail(
        user = "Jane Doe",
        loiLabel = "Map the farms",
        loiSubtitle = "IDX21311",
        mutation =
          SubmissionMutation(job = Job(id = "123"), syncStatus = Mutation.SyncStatus.PENDING),
      )
  }
}
