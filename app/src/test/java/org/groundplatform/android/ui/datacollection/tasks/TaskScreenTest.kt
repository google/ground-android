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
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.InstructionData
import org.groundplatform.android.ui.datacollection.components.LOI_NAME_TEXT_FIELD_TEST_TAG
import org.groundplatform.android.ui.datacollection.components.TaskHeader
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `renders header card when shouldShowHeader is true`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = TaskHeader("Test Header"),
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        shouldShowHeader = true,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = {},
        onAction = {},
        headerCard = { Text("Header Card Content") },
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
        shouldShowHeader = false,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = {},
        onAction = {},
        headerCard = null,
        taskBody = { Text("Task Body Content") },
      )
    }

    composeTestRule.onNodeWithText("Task Body Content").assertIsDisplayed()
  }

  @Test
  fun `renders footer actions and triggers callback`() {
    var actionFired: TaskScreenAction? = null

    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = listOf(ButtonActionState(ButtonAction.NEXT)),
        shouldShowLoiNameDialog = false,
        shouldShowHeader = false,
        showInstructionsDialog = false,
        loiName = "",
        onFooterPositionUpdated = {},
        onAction = { actionFired = it },
        headerCard = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText("Next").performClick()

    assertThat(actionFired).isEqualTo(TaskScreenAction.OnButtonClicked(ButtonAction.NEXT))
  }

  @Test
  fun `renders LoiNameDialog when shouldShowLoiNameDialog is true`() {
    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = true,
        shouldShowHeader = false,
        showInstructionsDialog = false,
        loiName = "My Custom LOI",
        onFooterPositionUpdated = {},
        onAction = {},
        headerCard = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText("My Custom LOI").assertIsDisplayed()
  }

  @Test
  fun `triggers LoiNameDialog callbacks`() {
    var actionFired: TaskScreenAction? = null

    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = null,
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = true,
        shouldShowHeader = false,
        showInstructionsDialog = false,
        loiName = "My Custom LOI",
        onFooterPositionUpdated = {},
        onAction = { actionFired = it },
        headerCard = null,
        taskBody = {},
      )
    }

    // Trigger explicit callbacks
    composeTestRule.onNodeWithText("Save").performClick()
    assertThat(actionFired).isEqualTo(TaskScreenAction.OnLoiNameConfirm("My Custom LOI"))

    composeTestRule.onNodeWithText("Cancel").performClick()
    assertThat(actionFired).isEqualTo(TaskScreenAction.OnLoiNameDismiss)

    composeTestRule.onNodeWithTag(LOI_NAME_TEXT_FIELD_TEST_TAG).performTextInput(" appended")
    assertThat(actionFired).isEqualTo(TaskScreenAction.OnLoiNameChanged("My Custom LOI appended"))
  }

  @Test
  fun `renders InstructionsDialog and triggers callback`() {
    var actionFired: TaskScreenAction? = null

    composeTestRule.setContent {
      TaskScreen(
        taskHeader = null,
        instructionData = InstructionData(R.drawable.ic_question_answer, R.string.add_point),
        taskActionButtonsStates = emptyList(),
        shouldShowLoiNameDialog = false,
        shouldShowHeader = false,
        showInstructionsDialog = true,
        loiName = "",
        onFooterPositionUpdated = {},
        onAction = { actionFired = it },
        headerCard = null,
        taskBody = {},
      )
    }

    composeTestRule.onNodeWithText("Close").performClick()
    assertThat(actionFired).isEqualTo(TaskScreenAction.OnInstructionsDismiss)
  }
}
