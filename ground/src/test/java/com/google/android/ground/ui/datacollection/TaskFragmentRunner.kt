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
package com.google.android.ground.ui.datacollection

import android.text.InputType
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.CustomViewActions.forceTypeText
import com.google.android.ground.R
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.allOf

/** Helper class for interacting with the data collection tasks and verifying the ui state. */
class TaskFragmentRunner(
  private val baseHiltTest: BaseHiltTest,
  private val fragment: DataCollectionFragment? = null,
) {

  internal fun clickNextButton(): TaskFragmentRunner = clickButton("Next")

  internal fun clickPreviousButton(): TaskFragmentRunner = clickButton("Previous")

  internal fun clickDoneButton(): TaskFragmentRunner = clickButton("Done")

  internal fun clickButton(
    text: String,
    isContentDescription: Boolean = false,
  ): TaskFragmentRunner {
    clickButtonInternal(text, isContentDescription)
    return this
  }

  internal fun inputText(text: String): TaskFragmentRunner {
    onView(allOf(withId(R.id.user_response_text), isDisplayed())).perform(typeText(text))
    return this
  }

  internal fun inputNumber(number: Int): TaskFragmentRunner {
    onView(withId(R.id.user_response_text))
      .check(matches(withInputType(InputType.TYPE_CLASS_NUMBER)))
      .perform(forceTypeText(number.toString()))
    return this
  }

  internal fun selectMultipleChoiceOption(optionText: String): TaskFragmentRunner {
    onView(withText(optionText)).perform(click())
    return this
  }

  internal fun validateTextIsDisplayed(text: String): TaskFragmentRunner {
    onView(withText(text)).check(matches(isDisplayed()))
    return this
  }

  internal fun validateTextIsNotDisplayed(text: String): TaskFragmentRunner {
    onView(withText(text)).check(matches(not(isDisplayed())))
    return this
  }

  internal fun pressBackButton(result: Boolean): TaskFragmentRunner {
    waitUntilDone { assertThat(fragment?.onBack()).isEqualTo(result) }
    return this
  }

  /**
   * Buttons in task fragments are using jetpack compose, so we have to use `ComposeTestRule` to
   * interact with the view.
   */
  private fun clickButtonInternal(text: String, isContentDescription: Boolean = false) =
    waitUntilDone {
      if (isContentDescription) {
        baseHiltTest.composeTestRule
          .onNode(hasContentDescription(text).and(isEnabled()))
          .performClick()
      } else {
        baseHiltTest.composeTestRule.onNode(hasText(text).and(isEnabled())).performClick()
      }
    }

  internal fun assertButtonIsDisabled(text: String): TaskFragmentRunner {
    baseHiltTest.composeTestRule.onNodeWithText(text).assertIsDisplayed().assertIsNotEnabled()
    return this
  }

  internal fun assertButtonIsEnabled(
    text: String,
    isContentDescription: Boolean = false,
  ): TaskFragmentRunner {
    if (isContentDescription) {
      baseHiltTest.composeTestRule
        .onNodeWithContentDescription(text)
        .assertIsDisplayed()
        .assertIsEnabled()
    } else {
      baseHiltTest.composeTestRule.onNodeWithText(text).assertIsDisplayed().assertIsEnabled()
    }
    return this
  }

  internal fun assertButtonIsHidden(
    text: String,
    isContentDescription: Boolean = false,
  ): TaskFragmentRunner {
    if (isContentDescription) {
      baseHiltTest.composeTestRule.onNodeWithContentDescription(text).assertIsNotDisplayed()
    } else {
      baseHiltTest.composeTestRule.onNodeWithText(text).assertIsNotDisplayed()
    }
    return this
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun waitUntilDone(testBody: suspend () -> Unit) {
    baseHiltTest.runWithTestDispatcher {
      testBody()
      advanceUntilIdle()
    }
  }
}
