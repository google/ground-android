/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.e2etest.drivers

import android.graphics.Point
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.groundplatform.android.R
import org.groundplatform.android.e2etest.TestConfig.DEFAULT_TIMEOUT
import org.groundplatform.android.e2etest.extensions.onTarget

@OptIn(ExperimentalTestApi::class)
class AndroidTestDriver(
  private val composeRule: AndroidComposeTestRule<*, *>,
  private val device: UiDevice,
) : TestDriver {
  private fun wait(target: TestDriver.Target, timeout: Long = DEFAULT_TIMEOUT) {
    when (target) {
      is TestDriver.Target.ContentDescription ->
        composeRule.waitUntilAtLeastOneExists(
          hasContentDescription(target.text) and isEnabled(),
          timeout,
        )

      is TestDriver.Target.TestTag ->
        composeRule.waitUntilAtLeastOneExists(hasTestTag(target.tag) and isEnabled(), timeout)

      is TestDriver.Target.Text ->
        composeRule.waitUntilAtLeastOneExists(
          hasText(target.text, target.substring) and isEnabled(),
          timeout,
        )

      is TestDriver.Target.ViewId -> {
        val resName = composeRule.activity.resources.getResourceEntryName(target.resId)
        val packageName = composeRule.activity.packageName
        val component = device.wait(Until.findObject(By.res(packageName, resName)), timeout)
        checkNotNull(component) { "Component not found after $timeout ms" }
      }
    }
  }

  override fun click(target: TestDriver.Target) {
    wait(target)
    if (target is TestDriver.Target.ViewId) {
      onView(withId(target.resId)).perform(ViewActions.click())
    } else {
      composeRule.onTarget(target).performClick()
    }
  }

  override fun selectFromList(target: TestDriver.Target, index: Int) {
    wait(target)
    if (target is TestDriver.Target.ViewId) {
      val resName = composeRule.activity.resources.getResourceEntryName(target.resId)
      val packageName = composeRule.activity.packageName
      val parent = device.findObject(By.res(packageName, resName))
      parent.children[index].click()
    } else {
      composeRule.onTarget(target, index).performClick()
    }
  }

  override fun dragMapBy(offsetX: Int, offsetY: Int) {
    wait(TestDriver.Target.ViewId(R.id.map))
    val resName = composeRule.activity.resources.getResourceEntryName(R.id.map)
    val packageName = composeRule.activity.packageName
    val map = device.findObject(By.res(packageName, resName))
    val center = map.visibleCenter

    map.drag(Point(center.x + offsetX, center.y + offsetY))
  }

  override fun clickMapMarker(description: String) {
    wait(TestDriver.Target.ViewId(R.id.map))
    val marker: UiObject = device.findObject(UiSelector().descriptionContains(description))
    marker.click()
  }

  override fun scrollTo(target: TestDriver.Target) {
    wait(target)
    if (target is TestDriver.Target.ViewId) {
      onView(withId(target.resId)).perform(ViewActions.scrollTo())
    } else {
      composeRule.onTarget(target).performScrollTo()
    }
  }

  override fun insertText(text: String, target: TestDriver.Target) {
    wait(target)
    if (target is TestDriver.Target.ViewId) {
      onView(withId(target.resId)).perform(ViewActions.typeText(text))
    } else {
      composeRule.onTarget(target).performTextInput(text)
    }
  }

  override fun takePhoto() {
    val shutterSelector = By.res("com.android.camera2:id/shutter_button")
    val doneSelector = By.res("com.android.camera2:id/done_button")

    val shutterButton = device.wait(Until.findObject(shutterSelector), DEFAULT_TIMEOUT)
    checkNotNull(shutterButton) { "Camera 'shutter button' not found after ${DEFAULT_TIMEOUT}ms" }
    shutterButton.click()
    val doneButton = device.wait(Until.findObject(doneSelector), DEFAULT_TIMEOUT)
    checkNotNull(doneButton) { "Camera 'done' button not found after ${DEFAULT_TIMEOUT}ms" }
    doneButton.click()
  }

  override fun setDate() {
    val resName = composeRule.activity.resources.getResourceEntryName(R.id.user_date_response_text)
    val packageName = composeRule.activity.packageName
    val textInputField = device.findObject(By.res(packageName, resName))
    textInputField?.click()

    device.wait(Until.findObject(By.clazz(DatePicker::class.java)), DEFAULT_TIMEOUT)
    device.findObject(By.text("OK")).click()
  }

  override fun setTime() {
    val resName = composeRule.activity.resources.getResourceEntryName(R.id.user_time_response_text)
    val packageName = composeRule.activity.packageName
    val textInputField = device.findObject(By.res(packageName, resName))
    textInputField?.click()

    device.wait(Until.findObject(By.clazz(TimePicker::class.java)), DEFAULT_TIMEOUT)
    device.findObject(By.text("OK")).click()
  }

  override fun getStringResource(id: Int): String = composeRule.activity.getString(id)
}
