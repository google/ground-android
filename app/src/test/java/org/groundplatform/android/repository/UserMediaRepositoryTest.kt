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
}
