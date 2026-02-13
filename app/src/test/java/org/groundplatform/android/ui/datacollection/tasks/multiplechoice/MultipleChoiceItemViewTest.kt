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
package org.groundplatform.android.ui.datacollection.tasks.multiplechoice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase.assertTrue
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.common.Constants
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MultipleChoiceItemViewTest : BaseHiltTest() {

  @Test
  fun `SELECT_ONE multiple choice shows radio buttons and toggles select event correctly`() {
    var clicked = false
    val item = generateMultipleChoiceItem(MultipleChoice.Cardinality.SELECT_ONE)

    composeTestRule.setContent {
      MultipleChoiceItemView(item = item, toggleItem = { clicked = true })
    }

    composeTestRule.onNodeWithTag(SELECT_MULTIPLE_RADIO_TEST_TAG).performClick()

    assertTrue(clicked)
    composeTestRule.onNodeWithTag(SELECT_MULTIPLE_CHECKBOX_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun `SELECT_MULTIPLE multiple choice shows checkbox and toggles select event correctly`() {
    var clicked = false
    val item = generateMultipleChoiceItem(MultipleChoice.Cardinality.SELECT_MULTIPLE)

    composeTestRule.setContent {
      MultipleChoiceItemView(item = item, toggleItem = { clicked = true })
    }

    composeTestRule.onNodeWithTag(SELECT_MULTIPLE_CHECKBOX_TEST_TAG).performClick()

    assertTrue(clicked)
    composeTestRule.onNodeWithTag(SELECT_MULTIPLE_RADIO_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun `Shows text field when isOtherOption is true and selected`() {
    val item =
      generateMultipleChoiceItem(
        MultipleChoice.Cardinality.SELECT_MULTIPLE,
        isOtherOption = true,
        isSelected = true,
      )

    composeTestRule.setContent { MultipleChoiceItemView(item = item) }

    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).assertIsDisplayed()
  }

  @Test
  fun `Hides text field when isOtherOption is false`() {
    val item =
      generateMultipleChoiceItem(
        MultipleChoice.Cardinality.SELECT_MULTIPLE,
        isOtherOption = false,
        isSelected = true,
      )

    composeTestRule.setContent { MultipleChoiceItemView(item = item) }

    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun `Changes in OtherTextField trigger otherValueChanged callback`() {
    val item =
      generateMultipleChoiceItem(
        MultipleChoice.Cardinality.SELECT_MULTIPLE,
        isOtherOption = true,
        isSelected = true,
      )
    var changedText: String? = null

    composeTestRule.setContent {
      MultipleChoiceItemView(item = item, otherValueChanged = { changedText = it })
    }

    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextInput("New text")

    assertThat(changedText).isEqualTo("New text")
  }

  @Test
  fun `Character counter displays correctly on OtherTextField`() {
    val otherText = "Test"
    val item =
      generateMultipleChoiceItem(
        MultipleChoice.Cardinality.SELECT_MULTIPLE,
        isOtherOption = true,
        isSelected = true,
        otherText = otherText,
      )

    composeTestRule.setContent { MultipleChoiceItemView(item = item) }

    composeTestRule
      .onNodeWithText("${otherText.length} / ${Constants.TEXT_DATA_CHAR_LIMIT}")
      .assertIsDisplayed()
  }

  private fun generateMultipleChoiceItem(
    cardinality: MultipleChoice.Cardinality,
    isSelected: Boolean = false,
    isOtherOption: Boolean = false,
    otherText: String = "",
  ): MultipleChoiceItem =
    MultipleChoiceItem(
      Option(id = "1", code = "code1", label = "Option 1"),
      cardinality = cardinality,
      isSelected = isSelected,
      isOtherOption = isOtherOption,
      otherText = otherText,
    )
}
