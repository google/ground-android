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

import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirebaseStorageManagerTest {
  private val rootReference: StorageReference = mock()
  private val childReference: StorageReference = mock()

  private val storageManager = FirebaseStorageManager().apply { storageReference = rootReference }

  @Test
  fun `getRemoteMediaPath nests the filename under the survey's submissions`() {
    val path = FirebaseStorageManager.getRemoteMediaPath("survey-1", "task-1-uuid.jpg")

    assertThat(path).isEqualTo("user-media/surveys/survey-1/submissions/task-1-uuid.jpg")
  }

  @Test
  fun `getRemoteMediaPath keeps survey and filename in separate path segments`() {
    val path = FirebaseStorageManager.getRemoteMediaPath("a/b", "c.jpg")

    // The survey id is interpolated verbatim; this pins the segment order, not any escaping.
    assertThat(path).isEqualTo("user-media/surveys/a/b/submissions/c.jpg")
  }

  @Test
  fun `getDownloadUrl resolves the url of the requested path`() = runTest {
    val expected = Uri.parse("https://example.com/user-media/photo.jpg")
    whenever(rootReference.child("user-media/surveys/s/submissions/photo.jpg"))
      .thenReturn(childReference)
    whenever(childReference.downloadUrl).thenReturn(Tasks.forResult(expected))

    val url = storageManager.getDownloadUrl("user-media/surveys/s/submissions/photo.jpg")

    assertThat(url).isEqualTo(expected)
  }
}
