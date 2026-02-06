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
package org.groundplatform.android.ui.datacollection.components.refactor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TaskFooterTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `Should display visible buttons`() {
    val states =
      listOf(
        ButtonActionState(action = ButtonAction.PREVIOUS, isVisible = true),
        ButtonActionState(action = ButtonAction.NEXT, isVisible = true),
      )
    composeTestRule.setContent { TaskFooter(buttonActionStates = states, onButtonClicked = {}) }

    composeTestRule.onNodeWithText("Previous").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next").assertIsDisplayed()
  }

  @Test
  fun `Should hide invisible buttons`() {
    val states =
      listOf(
        ButtonActionState(action = ButtonAction.PREVIOUS, isVisible = true),
        ButtonActionState(action = ButtonAction.NEXT, isVisible = false),
      )
    composeTestRule.setContent { TaskFooter(buttonActionStates = states, onButtonClicked = {}) }

    composeTestRule.onNodeWithText("Previous").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next").assertIsNotDisplayed()
  }

  @Test
  fun `Should propagates click events`() {
    var clickedAction: ButtonAction? = null
    val states = listOf(ButtonActionState(action = ButtonAction.NEXT, isVisible = true))
    composeTestRule.setContent {
      TaskFooter(buttonActionStates = states, onButtonClicked = { clickedAction = it })
    }

    composeTestRule.onNodeWithText("Next").performClick()

    assertThat(clickedAction).isEqualTo(ButtonAction.NEXT)
  }
}
