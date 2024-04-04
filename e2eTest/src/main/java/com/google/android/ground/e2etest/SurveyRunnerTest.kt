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

package com.google.android.ground.e2etest

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.android.ground.model.task.Task
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.fail
import org.junit.Test
import org.junit.runner.RunWith

private const val GROUND_PACKAGE = "com.google.android.ground"
private const val LONG_TIMEOUT = 5000L
private const val SHORT_TIMEOUT = 2000L

@RunWith(AndroidJUnit4::class)
class SurveyRunnerTest {

  private lateinit var device: UiDevice

  @Test
  fun run() {
    launchGroundFromHomeScreen()
    signIn()
    selectSurvey()
    zoomIntoLocation()
    startLoiTask()
    collectData(
      isAdHoc = true,
      listOf(
        Task.Type.DROP_PIN,
        Task.Type.TEXT,
        Task.Type.MULTIPLE_CHOICE,
        Task.Type.MULTIPLE_CHOICE,
        Task.Type.NUMBER,
        Task.Type.DATE,
        Task.Type.TIME,
        Task.Type.PHOTO,
        Task.Type.CAPTURE_LOCATION,
      ),
    )
  }

  private fun launchGroundFromHomeScreen() {
    // Initialize UiDevice instance
    device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    // Start from the home screen
    device.pressHome()

    // Wait for launcher
    val launcherPackage: String = device.launcherPackageName
    assertThat(launcherPackage).isNotNull()
    device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LONG_TIMEOUT)

    // Launch the app
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent =
      context.packageManager.getLaunchIntentForPackage(GROUND_PACKAGE)?.apply {
        // Clear out any previous instances
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
    context.startActivity(intent)

    // Wait for the app to appear
    if (!device.wait(Until.hasObject(By.pkg(GROUND_PACKAGE).depth(0)), LONG_TIMEOUT)) {
      fail("Failed to launch app.")
    }
  }

  private fun signIn() {
    if (!waitClickGone(By.textContains("Sign in"), LONG_TIMEOUT)) {
      fail("Failed to sign in.")
    }
  }

  private fun selectSurvey() {
    device.wait(Until.hasObject(By.textContains("survey")), LONG_TIMEOUT)
    val testSurveySelector =
      By.clazz("androidx.cardview.widget.CardView")
        .hasDescendant(By.clazz("android.widget.TextView").textContains("test"))
    device.wait(Until.hasObject(testSurveySelector), LONG_TIMEOUT)
    // Need to double click on survey.
    waitClickGone(testSurveySelector)
    if (!waitClickGone(testSurveySelector, timeout = LONG_TIMEOUT)) {
      fail("Failed to select survey.")
    }
  }

  private fun zoomIntoLocation() {
    clickLocationLock()
    allowPermissions()
    val dataCollectionCardSelector =
      By.clazz("androidx.cardview.widget.CardView")
        .hasDescendant(By.res("com.google.android.ground", "collectData"))
    if (device.wait(Until.hasObject(dataCollectionCardSelector), LONG_TIMEOUT) == null) {
      fail("Failed to zoom in to location.")
    }
  }

  private fun startLoiTask() {
    val dataCollectionCardSelector =
      By.clazz("androidx.cardview.widget.CardView")
        .hasDescendant(By.res("com.google.android.ground", "collectData"))
    device.wait(Until.hasObject(dataCollectionCardSelector), LONG_TIMEOUT)
    val cards = device.findObjects(dataCollectionCardSelector)
    cards.forEach {
      if (it.resourceName != "com.google.android.ground:id/loi_card") {
        it.swipe(Direction.LEFT, 1F)
      }
    }
    val loiCollectDataButtonSelector =
      By.res("com.google.android.ground", "collectData")
        .hasAncestor(By.res("com.google.android.ground", "loi_card"))
    if (!waitClickGone(loiCollectDataButtonSelector)) {
      fail("Failed to start data collection.")
    }
  }

  private fun collectData(isAdHoc: Boolean, taskList: List<Task.Type>) {
    taskList.forEachIndexed { i, it ->
      device.waitForIdle()
      when (it) {
        Task.Type.DROP_PIN -> completeDropPinTask()
        Task.Type.DRAW_AREA -> completeDrawArea()
        Task.Type.CAPTURE_LOCATION -> completeCaptureLocation()
        Task.Type.MULTIPLE_CHOICE -> completeMultipleChoice()
        Task.Type.TEXT -> completeText()
        Task.Type.PHOTO -> completePhoto()
        Task.Type.NUMBER -> completeNumber()
        Task.Type.DATE -> completeDate()
        Task.Type.TIME -> completeTime()
        Task.Type.UNKNOWN -> fail("Should not get here")
      }
      if (i < taskList.size - 1) {
        clickNext()
        if (isAdHoc && i == 0) {
          setLoiName()
        }
      } else {
        clickDone()
      }
    }
  }

  private fun completeDropPinTask() {
    // Instructions dialog may be triggered.
    waitClickGone(By.text("Close"))
    waitClickGone(By.text("Drop pin"))
  }

  private fun completeDrawArea() {
    TODO()
  }

  private fun completeCaptureLocation() {
    waitClickGone(By.text("Capture"))
  }

  private fun completeMultipleChoice() {
    val radioSelector = By.clazz("android.widget.RadioButton")
    val checkBoxSelector = By.clazz("android.widget.CheckBox")
    val optionSelector = if (device.hasObject(radioSelector)) radioSelector else checkBoxSelector
    val options = device.findObjects(optionSelector)
    // Ensure that the first option is selected last.
    options.reversed().forEach { it.click() }
    if (hasTextField()) {
      enterText("An other option")
    }
  }

  private fun completeText() {
    enterText("A text answer")
  }

  private fun completePhoto() {
    allowPermissions()
    waitClickGone(By.text("Camera"))
    waitClickGone(By.res("com.android.camera2:id/shutter_button"))
    waitClickGone(By.res("com.android.camera2:id/done_button"))
  }

  private fun completeNumber() {
    enterText("1234")
  }

  private fun completeDate() {
    device.findObject(By.clazz("android.widget.EditText")).click()
    waitClickGone(By.text("OK"))
  }

  private fun completeTime() {
    device.findObject(By.clazz("android.widget.EditText")).click()
    waitClickGone(By.text("OK"))
  }

  private fun clickNext() {
    waitClickGone(By.text("Next"))
  }

  private fun clickDone() {
    waitClickGone(By.text("Done"))
  }

  private fun clickLocationLock() {
    waitClickGone(By.res("com.google.android.ground", "location_lock_btn"), timeout = LONG_TIMEOUT)
  }

  private fun allowPermissions() {
    waitClickGone(By.textContains("While using the app"))
  }

  private fun setLoiName() {
    device.wait(Until.hasObject(By.text("Save")), SHORT_TIMEOUT)
    enterText("An loi name")
    waitClickGone(By.text("Save"))
  }

  private fun hasTextField() = device.hasObject(By.clazz("android.widget.EditText"))

  private fun waitClickGone(
    selector: BySelector,
    timeout: Long = SHORT_TIMEOUT,
  ): Boolean {
    device.wait(Until.hasObject(selector), timeout)
    device.findObject(selector)?.click()
    return device.wait(Until.gone(selector), timeout)
  }

  private fun enterText(text: String) {
    val textSelector = By.clazz("android.widget.EditText")
    device.wait(Until.hasObject(textSelector), SHORT_TIMEOUT)
    device.findObject(textSelector).text = text
  }
}
