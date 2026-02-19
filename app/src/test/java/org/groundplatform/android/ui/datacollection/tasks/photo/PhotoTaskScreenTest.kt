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
package org.groundplatform.android.ui.datacollection.tasks.photo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.BaseHiltTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w600dp-h1024dp")
class PhotoTaskScreenTest : BaseHiltTest() {

  @Test
  fun `shows capture button when photo is not present`() {
    composeTestRule.setContent { PhotoTaskScreen(uri = null, onTakePhoto = {}) }

    composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Preview").assertIsNotDisplayed()
  }

  @Test
  fun `shows photo preview when photo is present`() {
    composeTestRule.setContent {
      PhotoTaskScreen(uri = "content://media/external/images/media/1".toUri(), onTakePhoto = {})
    }

    composeTestRule.onNodeWithText("Camera").assertIsNotDisplayed()
    composeTestRule.onNodeWithContentDescription("Preview").assertIsDisplayed()
  }

  @Test
  fun `invokes onTakePhoto callback when capture button is clicked`() {
    var onTakePhotoCalled = false

    composeTestRule.setContent {
      PhotoTaskScreen(uri = null, onTakePhoto = { onTakePhotoCalled = true })
    }

    composeTestRule.onNodeWithText("Camera").performClick()

    assertThat(onTakePhotoCalled).isTrue()
  }
}
