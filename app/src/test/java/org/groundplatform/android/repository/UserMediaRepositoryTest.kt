/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.repository

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class UserMediaRepositoryTest : BaseHiltTest() {

  @Inject lateinit var userMediaRepository: UserMediaRepository

  @Test
  fun `getLocalFileFromRemotePath handles invalid image filename`() {
    for (path in
      listOf(
        "(/some/path/filename.png)",
        "/some/path/filename.",
        "/some/path/filename",
        "/some/path/filename.txt",
        "/some/path/filename$.png",
      )) {
      assertThrows(IllegalArgumentException::class.java) {
        userMediaRepository.getLocalFileFromRemotePath(path)
      }
    }
  }

  @Test
  fun `getLocalFileFromRemotePath handles valid image filename`() {
    for (path in listOf("/some/path/filename.png", "/some/path/filename.jpg")) {
      val localFile = userMediaRepository.getLocalFileFromRemotePath(path)
      assertThat(localFile).isNotNull()
    }
  }

  @Test
  fun `createImageFile returns path with fieldId, uuid and jpg extension`() =
    runWithTestDispatcher {
      val mediaFile = userMediaRepository.createImageFile("field1")

      assertThat(File(mediaFile).name).isEqualTo("field1-TEST UUID.jpg")
    }

  @Test
  fun `createImageFile returns distinct names for different fieldIds`() = runWithTestDispatcher {
    val first = userMediaRepository.createImageFile("fieldA")
    val second = userMediaRepository.createImageFile("fieldB")

    assertThat(first).isNotEqualTo(second)
  }

  @Test
  fun `getDownloadUrl returns empty when path is null`() = runWithTestDispatcher {
    assertThat(userMediaRepository.getDownloadUrl(null)).isEmpty()
  }

  @Test
  fun `getDownloadUrl returns empty when path is empty`() = runWithTestDispatcher {
    assertThat(userMediaRepository.getDownloadUrl("")).isEmpty()
  }

  @Test
  fun `getDownloadUrl falls back to remote when local file is missing`() = runWithTestDispatcher {
    assertThat(userMediaRepository.getDownloadUrl("remote/path/missing.jpg")).isEmpty()
  }

  @Test
  fun `getDownloadUrl returns local file uri when file exists`() = runWithTestDispatcher {
    val mediaFile = userMediaRepository.createImageFile("field1")
    File(mediaFile).writeBytes(ByteArray(0))

    val downloadUrl = userMediaRepository.getDownloadUrl("remote/path/${File(mediaFile).name}")

    assertThat(downloadUrl).startsWith("file://")
    assertThat(downloadUrl).endsWith(File(mediaFile).name.replace(" ", "%20"))
  }

  @Test
  fun `getUriForFile returns content uri backed by FileProvider`() = runWithTestDispatcher {
    val mediaFile = userMediaRepository.createImageFile("field1")
    File(mediaFile).writeBytes(ByteArray(0))

    val uri = userMediaRepository.getUriForFile(mediaFile)

    assertThat(uri).startsWith("content://org.groundplatform.android/")
    assertThat(uri).endsWith(File(mediaFile).name.replace(" ", "%20"))
  }
}
