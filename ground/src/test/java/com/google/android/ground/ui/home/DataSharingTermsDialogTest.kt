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
package com.google.android.ground.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.proto.Survey
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.test.Test
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataSharingTermsDialogTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun dataSharingTermsDialog_DisplaysTitleCorrectly() {
    val showDialog = mutableStateOf(true)

    composeTestRule.setContent {
      DataSharingTermsDialog(
        showDataSharingTermsDialog = showDialog,
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder().setType(Survey.DataSharingTerms.Type.PRIVATE).build(),
      )
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.data_consent_dialog_title))
      .assertIsDisplayed()
  }

  @Test
  fun dataSharingTermsDialog_DisplaysCorrectMessageForPrivateTerms() {
    val showDialog = mutableStateOf(true)

    composeTestRule.setContent {
      DataSharingTermsDialog(
        showDataSharingTermsDialog = showDialog,
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder().setType(Survey.DataSharingTerms.Type.PRIVATE).build(),
      )
    }
    val markdown = composeTestRule.activity.getString(R.string.data_sharing_private_message)
    val generatedHtml = generateHtmlFromMarkdown(markdown)
    val expectedHtml =
      "<body><h2>Private data sharing</h2><p>Data will only be shared with survey organizers, " +
        "who may not share and use collected data publicly.</p></body>"
    assertEquals(expectedHtml, generatedHtml.trim())
  }

  @Test
  fun dataSharingTermsDialog_DisplaysCorrectMessageForPublicTerms() {
    val showDialog = mutableStateOf(true)

    composeTestRule.setContent {
      DataSharingTermsDialog(
        showDataSharingTermsDialog = showDialog,
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder()
            .setType(Survey.DataSharingTerms.Type.PUBLIC_CC0)
            .build(),
      )
    }

    val markdown = composeTestRule.activity.getString(R.string.data_sharing_public_message)
    val generatedHtml = generateHtmlFromMarkdown(markdown)
    val expectedHtml =
      "<body><h2>Public data sharing</h2><p>Survey organizers may share and use data publicly under" +
        " the <em>Creative Commons CC0 1.0 License</em>:</p></body>"
    assertEquals(expectedHtml, generatedHtml.trim())
  }

  @Test
  fun dataSharingTermsDialog_DisplaysCorrectMessageForCustomTerms() {
    val showDialog = mutableStateOf(true)

    composeTestRule.setContent {
      DataSharingTermsDialog(
        showDataSharingTermsDialog = showDialog,
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder()
            .setType(Survey.DataSharingTerms.Type.CUSTOM)
            .setCustomText("Custom text")
            .build(),
      )
    }

    val generatedHtml = generateHtmlFromMarkdown("Custom text")
    val expectedHtml = "<body><p>Custom text</p></body>"
    assertEquals(expectedHtml, generatedHtml.trim())
  }

  @Test
  fun dataSharingTermsDialog_DisplaysCorrectMessageForNoTerms() {
    val showDialog = mutableStateOf(true)

    composeTestRule.setContent {
      DataSharingTermsDialog(
        showDataSharingTermsDialog = showDialog,
        dataSharingTerms = Survey.DataSharingTerms.getDefaultInstance(),
      )
    }

    val markdown = composeTestRule.activity.getString(R.string.data_sharing_no_terms)
    val generatedHtml = generateHtmlFromMarkdown(markdown)
    val expectedHtml = "<body><p><em>No terms to display.</em></p></body>"
    assertEquals(expectedHtml, generatedHtml.trim())
  }

  @Test
  fun dataSharingTermsDialog_DismissesOnCancelClick() {
    val showDialog = mutableStateOf(true)

    composeTestRule.setContent {
      DataSharingTermsDialog(
        showDataSharingTermsDialog = showDialog,
        dataSharingTerms =
          Survey.DataSharingTerms.newBuilder().setType(Survey.DataSharingTerms.Type.PRIVATE).build(),
      )
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .performClick()

    assertFalse(showDialog.value)
  }

  @Test
  fun dataSharingTermsDialog_CallsConsentGivenCallback_OnAgreeClick() {
    var callbackCalled = false
    val showDialog = mutableStateOf(true)

    composeTestRule.setContent {
      DataSharingTermsDialog(
        showDataSharingTermsDialog = showDialog,
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

    assertTrue(callbackCalled)

    assertFalse(showDialog.value)
  }

  private fun generateHtmlFromMarkdown(markdown: String): String {
    val flavor = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavor).buildMarkdownTreeFromString(markdown)
    return HtmlGenerator(markdown, parsedTree, flavor).generateHtml()
  }
}
