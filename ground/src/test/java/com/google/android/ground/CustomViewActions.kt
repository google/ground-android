/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground

import android.view.View
import android.widget.EditText
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf.allOf

object CustomViewActions {

  /**
   * For an EditText with inputType="number", Espresso's typeText() doesn't have any impact when run
   * on robolectric.
   *
   * Tracking bug: https://github.com/robolectric/robolectric/issues/5110
   */
  internal fun forceTypeText(text: String): ViewAction =
    object : ViewAction {
      override fun getDescription(): String = "force type text"

      override fun getConstraints(): Matcher<View> = allOf(isEnabled())

      override fun perform(uiController: UiController?, view: View?) {
        (view as? EditText)?.append(text)
        uiController?.loopMainThreadUntilIdle()
      }
    }

  /** Removes number of characters from text input. */
  internal fun clearText(): ViewAction =
    object : ViewAction {
      override fun getDescription(): String = "force type text"

      override fun getConstraints(): Matcher<View> = allOf(isEnabled())

      override fun perform(uiController: UiController?, view: View?) {
        (view as? EditText)?.setText("")
        uiController?.loopMainThreadUntilIdle()
      }
    }
}
