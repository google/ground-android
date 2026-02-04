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
package org.groundplatform.android.ui.datacollection.components.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertNull
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TaskButtonTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `Should display text content correctly`() {
    val state = ButtonActionState(action = ButtonAction.NEXT, isEnabled = true)
    composeTestRule.setContent { TaskButton(state = state, onClick = {}) }

    composeTestRule.onNodeWithText("Next").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun `Should display icon content correctly`() {
    val state = ButtonActionState(action = ButtonAction.UNDO, isEnabled = true)
    composeTestRule.setContent { TaskButton(state = state, onClick = {}) }

    composeTestRule.onNodeWithContentDescription("Undo").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun `Should fire onClick event`() {
    var clickedAction: ButtonAction? = null
    val state = ButtonActionState(action = ButtonAction.NEXT, isEnabled = true)
    composeTestRule.setContent { TaskButton(state = state, onClick = { clickedAction = it }) }

    composeTestRule.onNodeWithText("Next").performClick()

    assertThat(clickedAction).isEqualTo(ButtonAction.NEXT)
  }

  @Test
  fun `Should respect disabled state`() {
    var clickedAction: ButtonAction? = null
    val state = ButtonActionState(action = ButtonAction.NEXT, isEnabled = false)
    composeTestRule.setContent { TaskButton(state = state, onClick = { clickedAction = it }) }

    composeTestRule.onNodeWithText("Next").performClick()
    composeTestRule.onNodeWithText("Next").assertIsDisplayed().assertIsNotEnabled()
    assertNull(clickedAction)
  }

  @Test
  fun `Should respect enabled state`() {
    val state = ButtonActionState(action = ButtonAction.NEXT, isEnabled = true)
    composeTestRule.setContent { TaskButton(state = state, onClick = {}) }

    composeTestRule.onNodeWithText("Next").assertIsDisplayed().assertIsEnabled()
  }
}
