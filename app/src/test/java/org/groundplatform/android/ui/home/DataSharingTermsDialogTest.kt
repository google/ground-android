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
package org.groundplatform.android.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.Test
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.proto.Survey
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataSharingTermsDialogTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `title is displayed`() {
    composeTestRule.setContent {
      DataSharingTermsDialog(
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder().setType(Survey.DataSharingTerms.Type.PRIVATE).build()
      )
    }

    assertDialogVisible(true)
  }

  @Test
  fun `verify private data sharing terms`() {
    composeTestRule.setContent {
      DataSharingTermsDialog(
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder().setType(Survey.DataSharingTerms.Type.PRIVATE).build()
      )
    }

    val expectedHtml =
      "<body><h2>Private data sharing</h2><p>Data will only be shared with survey organizers, " +
        "who may not share and use collected data publicly.</p></body>"
    composeTestRule.onNodeWithText(expectedHtml).isDisplayed()
  }

  @Test
  fun `verify public data sharing terms`() {
    composeTestRule.setContent {
      DataSharingTermsDialog(
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder()
            .setType(Survey.DataSharingTerms.Type.PUBLIC_CC0)
            .build()
      )
    }

    val expectedHtml =
      "<body><h2>Public data sharing</h2><p>Survey organizers may share and use data publicly under" +
        " the <em>Creative Commons CC0 1.0 License</em>:</p></body>"
    composeTestRule.onNodeWithText(expectedHtml).isDisplayed()
  }

  @Test
  fun `verify custom data sharing terms`() {
    composeTestRule.setContent {
      DataSharingTermsDialog(
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder()
            .setType(Survey.DataSharingTerms.Type.CUSTOM)
            .setCustomText("Custom text")
            .build()
      )
    }

    val expectedHtml = "<body><p>Custom text</p></body>"
    composeTestRule.onNodeWithText(expectedHtml).isDisplayed()
  }

  @Test
  fun `verify message for no terms`() {
    composeTestRule.setContent {
      DataSharingTermsDialog(dataSharingTerms = Survey.DataSharingTerms.getDefaultInstance())
    }

    val expectedHtml = "<body><p><em>No terms to display.</em></p></body>"
    composeTestRule.onNodeWithText(expectedHtml).isDisplayed()
  }

  @Test
  fun `cancel button click dismisses the dialog`() {
    composeTestRule.setContent {
      DataSharingTermsDialog(
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder().setType(Survey.DataSharingTerms.Type.PRIVATE).build()
      )
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .performClick()

    assertDialogVisible(false)
  }

  @Test
  fun `agree button click invokes consent callback`() {
    var callbackCalled = false

    composeTestRule.setContent {
      DataSharingTermsDialog(
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder()
            .setType(Survey.DataSharingTerms.Type.PRIVATE)
            .build(),
        consentGivenCallback = { callbackCalled = true },
      )
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.agree_checkbox))
      .performClick()

    assertThat(callbackCalled).isTrue()
    assertDialogVisible(false)
  }

  private fun assertDialogVisible(isVisible: Boolean) {
    val title = composeTestRule.activity.getString(R.string.data_consent_dialog_title)
    val node = composeTestRule.onNodeWithText(title)
    if (isVisible) {
      node.assertIsDisplayed()
    } else {
      node.assertIsNotDisplayed()
    }
  }
}
