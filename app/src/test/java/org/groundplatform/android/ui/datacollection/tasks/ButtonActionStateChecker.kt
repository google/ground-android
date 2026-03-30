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

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.groundplatform.android.getString
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState

/**
 * Helper class for verifying the state of action buttons in the UI during Compose tests.
 *
 * This class provides utility methods to assert whether specific buttons are visible, hidden,
 * enabled, or disabled based on a set of [ButtonActionState] definitions.
 *
 * @property composeTestRule The [ComposeTestRule] used to interact with and find Compose nodes.
 */
class ButtonActionStateChecker(private val composeTestRule: ComposeTestRule) {

  fun assertButtonStates(vararg buttonStates: ButtonActionState) {
    buttonStates.forEach { assertButtonState(it) }
  }

  fun assertButtonState(state: ButtonActionState) {
    val node = getNode(state.action)

    if (state.isVisible) {
      node.assertExists().assertIsDisplayed()
      if (state.isEnabled) {
        node.assertIsEnabled()
      } else {
        node.assertIsNotEnabled()
      }
    } else {
      node.assertDoesNotExist()
    }
  }

  fun getNode(buttonAction: ButtonAction): SemanticsNodeInteraction =
    when {
      buttonAction.contentDescription != null -> {
        composeTestRule.onNodeWithContentDescription(getString(buttonAction.contentDescription))
      }
      buttonAction.textId != null -> {
        composeTestRule.onNodeWithText(getString(buttonAction.textId))
      }
      else -> error("Invalid button action: $buttonAction")
    }
}
