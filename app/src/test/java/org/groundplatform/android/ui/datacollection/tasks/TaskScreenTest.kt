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
package org.groundplatform.android.ui.datacollection.tasks

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.LOI_NAME_TEXT_FIELD_TEST_TAG
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.groundplatform.android.ui.datacollection.LoiNameAction

@RunWith(RobolectricTestRunner::class)
class TaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `renders footer header when footerHeader is not null`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = TaskHeader("Test Header"),
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = {},
        footerContent = { Text("Header Card Content") },
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText("Header Card Content").assertIsDisplayed()
  }

  @Test
  fun `renders task body`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = {},
        footerContent = null,
        taskBody = { Text("Task Body Content") },
      )
    }

    composeTestRule.onNodeWithText("Task Body Content").assertIsDisplayed()
  }

  @Test
  fun `renders footer actions and triggers callback`() {
    var actionFired: ButtonAction? = null

    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = listOf(ButtonActionState(ButtonAction.NEXT)),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = {},
        onButtonClicked = { actionFired = it },
        footerContent = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText("Next").performClick()

    assertThat(actionFired).isEqualTo(ButtonAction.NEXT)
  }

  @Test
  fun `renders LoiNameDialog when shouldShowLoiNameDialog is true`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = true,
        showInstructionsDialog = false,
        loiName = "My Custom LOI",
        onFooterPositionUpdated = {},
        footerContent = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText("My Custom LOI").assertIsDisplayed()
  }

  @Test
  fun `triggers LoiNameDialog callbacks`() {
    var confirmedName: String? = null
    var dismissed = false
    var changedName: String? = null

    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = true,
        showInstructionsDialog = false,
        loiName = "My Custom LOI",
        onFooterPositionUpdated = {},
        onLoiNameAction = { action ->
          when (action) {
            is LoiNameAction.Confirmed -> confirmedName = action.name
            is LoiNameAction.Dismissed -> dismissed = true
            is LoiNameAction.Changed -> changedName = action.name
          }
        },
        footerContent = null,
        taskBody = {},
      )
    }

    // Trigger explicit callbacks
    composeTestRule.onNodeWithText("Save").performClick()
    assertThat(confirmedName).isEqualTo("My Custom LOI")

    composeTestRule.onNodeWithText("Cancel").performClick()
    assertThat(dismissed).isTrue()

    composeTestRule.onNodeWithTag(LOI_NAME_TEXT_FIELD_TEST_TAG).performTextInput("appended ")
    assertThat(changedName).isEqualTo("appended My Custom LOI")
  }

  @Test
  fun `renders InstructionsDialog and triggers callback`() {
    var instructionsDismissed = false

    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = InstructionData(R.drawable.ic_question_answer, R.string.add_point),
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = true,
        loiName = "",
        onFooterPositionUpdated = {},
        onInstructionsDismiss = { instructionsDismissed = true },
        footerContent = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText(getString(R.string.add_point)).assertIsDisplayed()
    composeTestRule.onNodeWithText("Close").performClick()

    assertThat(instructionsDismissed).isTrue()
  }

  @Test
  fun `triggers onFooterPositionUpdated when layout coordinates change`() {
    var footerPosition = -1f

    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = { footerPosition = it },
        footerContent = null,
        taskBody = {},
      )
    }

    // Compose will do a layout pass and call onGloballyPositioned,
    // which in turn updates the layoutCoordinates state and triggers LaunchedEffect.
    composeTestRule.waitForIdle()

    // Asserts that the callback was fired and a layout coordinate window position was provided.
    assertThat(footerPosition).isAtLeast(0f)
  }

  @Test
  fun `does not render footer header when footerHeader is null`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = TaskHeader("Test Header"),
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = {},
        footerContent = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText("Header Card Content").assertDoesNotExist()
  }

  @Test
  fun `does not render LoiNameDialog when shouldShowLoiNameDialog is false`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = false,
        loiName = "My Custom LOI",
        onFooterPositionUpdated = {},
        footerContent = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText("My Custom LOI").assertDoesNotExist()
  }

  @Test
  fun `does not render InstructionsDialog when showInstructionsDialog is false`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = InstructionData(R.drawable.ic_question_answer, R.string.add_point),
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = {},
        footerContent = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText(getString(R.string.add_point)).assertDoesNotExist()
  }

  @Test
  fun `does not render InstructionsDialog when instructionData is null`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        showInstructionsDialog = true, // Expected to show, but data is null
        loiName = "",
        onFooterPositionUpdated = {},
        footerContent = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText(getString(R.string.add_point)).assertDoesNotExist()
  }
}
