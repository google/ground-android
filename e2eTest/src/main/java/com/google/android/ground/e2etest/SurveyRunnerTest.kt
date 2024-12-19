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

import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.test.core.app.takeScreenshot
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.android.ground.R
import com.google.android.ground.e2etest.TestConfig.GROUND_PACKAGE
import com.google.android.ground.e2etest.TestConfig.LONG_TIMEOUT
import com.google.android.ground.e2etest.TestConfig.SHORT_TIMEOUT
import com.google.android.ground.e2etest.TestConfig.TEST_SURVEY_IDENTIFIER
import com.google.android.ground.e2etest.TestConfig.TEST_SURVEY_LOI_TASK_INDEX
import com.google.android.ground.e2etest.TestConfig.TEST_SURVEY_TASKS_ADHOC
import com.google.android.ground.model.task.Task
import java.io.IOException
import junit.framework.TestCase.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SurveyRunnerTest : AutomatorRunner {

  @get:Rule var nameRule = TestName()

  override lateinit var device: UiDevice

  @Test
  fun run() {
    launchGround()
    signIn()
    selectTestSurvey()
    zoomIntoLocation()
    startAdHocLoiTask()
    fillOutTaskData(isAdHoc = true, TEST_SURVEY_TASKS_ADHOC)
    clickSubmissionConfirmationDone()
    startPredefinedLoiTask()
    fillOutTaskData(isAdHoc = false, TEST_SURVEY_TASKS_ADHOC)
    clickSubmissionConfirmationDone()
  }

  private fun launchGround() {
    // Initialize UiDevice instance.
    device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    launchPackage(GROUND_PACKAGE)

    // Wait for the app to appear.
    if (!device.wait(Until.hasObject(By.pkg(GROUND_PACKAGE).depth(0)), LONG_TIMEOUT)) {
      captureScreenshot()
      fail("Failed to launch app.")
    }
    device.wait(Until.hasObject(byText(R.string.initializing)), SHORT_TIMEOUT)
    if (device.wait(Until.gone(byText(R.string.initializing)), LONG_TIMEOUT) == null) {
      captureScreenshot()
      fail("Timed out while initializing.")
    }
  }

  private fun signIn() {
    if (!waitClickGone(byClass(Button::class), LONG_TIMEOUT)) {
      captureScreenshot()
      fail("Failed to sign in.")
    }
  }

  private fun selectTestSurvey() {
    if (device.wait(Until.hasObject(byText(R.string.select_survey_title)), LONG_TIMEOUT) == null) {
      captureScreenshot()
      fail("Failed to find select survey title")
    }
    val testSurveySelector =
      byClass(CardView::class)
        .hasDescendant(byClass(TextView::class).textContains(TEST_SURVEY_IDENTIFIER))
    if (device.wait(Until.hasObject(testSurveySelector), LONG_TIMEOUT) == null) {
      captureScreenshot()
      fail("Failed to find test survey")
    }
    // Need to double click on survey.
    waitClickGone(testSurveySelector)
    if (!waitClickGone(testSurveySelector, timeout = LONG_TIMEOUT)) {
      captureScreenshot()
      fail("Failed to select survey.")
    }
  }

  private fun zoomIntoLocation() {
    clickLocationLock()
    allowPermissions()
    val loiCardSelector = byClass(CardView::class).hasDescendant(byText(R.string.add_data))
    if (device.wait(Until.hasObject(loiCardSelector), LONG_TIMEOUT) == null) {
      captureScreenshot()
      fail("Failed to zoom in to location.")
    }
  }

  private fun startAdHocLoiTask() {
    val loiCardSelector = byClass(CardView::class)
    if (device.wait(Until.hasObject(loiCardSelector), LONG_TIMEOUT) == null) {
      captureScreenshot()
      fail("Failed to find ad-hoc loi card")
    }
    val cards = device.findObjects(loiCardSelector)
    cards.forEach { it.swipe(Direction.LEFT, 1F) }
    val loiCollectDataButtonSelector =
      byText(R.string.add_data)
        .hasAncestor(loiCardSelector.hasDescendant(byText(R.string.add_site)))
    if (!waitClickGone(loiCollectDataButtonSelector)) {
      captureScreenshot()
      fail("Failed to start ad-hoc loi data collection.")
    }
  }

  private fun startPredefinedLoiTask() {
    val loiCardSelector = byClass(CardView::class)
    if (device.wait(Until.hasObject(loiCardSelector), LONG_TIMEOUT) == null) {
      captureScreenshot()
      fail("Failed to find predefined loi card")
    }
    // Assume that the first card is the predefined LOI.
    val loiCollectDataButtonSelector = byText(R.string.add_data)
    if (!waitClickGone(loiCollectDataButtonSelector)) {
      captureScreenshot()
      fail("Failed to start predefined loi data collection.")
    }
  }

  private fun fillOutTaskData(isAdHoc: Boolean, taskList: List<Task.Type>) {
    taskList.forEachIndexed { i, it ->
      device.waitForIdle()
      if (!isAdHoc && i == TEST_SURVEY_LOI_TASK_INDEX) {
        return@forEachIndexed
      }
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
        if (isAdHoc && i == TEST_SURVEY_LOI_TASK_INDEX) {
          setLoiName()
        }
      } else {
        clickDone()
      }
    }
  }

  private fun completeDropPinTask() {
    // Instructions dialog may be triggered.
    waitClickGone(byText(R.string.close))
    waitClickGone(byText(R.string.drop_pin))
  }

  private fun completeDrawArea() {
    // Instructions dialog may be triggered.
    waitClickGone(byText(R.string.close))
    waitClickGone(byText(R.string.add_point))
    waitClickGone(byText(R.string.add_point))
    waitClickGone(byText(R.string.add_point))
    waitClickGone(byText(R.string.complete_polygon))
  }

  private fun completeCaptureLocation() {
    waitClickGone(byText(R.string.capture))
  }

  private fun completeMultipleChoice() {
    val radioSelector = byClass(RadioButton::class)
    val checkBoxSelector = byClass(CheckBox::class)
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
    waitClickGone(byText(R.string.camera))
    waitClickGone(By.res("com.android.camera2:id/shutter_button"))
    waitClickGone(By.res("com.android.camera2:id/done_button"))
  }

  private fun completeNumber() {
    enterText("1234")
  }

  private fun completeDate() {
    device.findObject(byClass(EditText::class)).click()
    waitClickGone(byText(R.string.ok))
  }

  private fun completeTime() {
    device.findObject(byClass(EditText::class)).click()
    waitClickGone(byText(R.string.ok))
  }

  private fun clickNext() {
    waitClickGone(byText(R.string.next))
  }

  private fun clickDone() {
    waitClickGone(byText(R.string.done))
  }

  private fun clickSubmissionConfirmationDone() {
    waitClickGone(byText(R.string.done), LONG_TIMEOUT)
  }

  private fun clickLocationLock() {
    waitClickGone(By.res("com.google.android.ground", "location_lock_btn"), timeout = LONG_TIMEOUT)
  }

  private fun setLoiName() {
    captureScreenshot()
    if (device.wait(Until.hasObject(byText(R.string.save)), SHORT_TIMEOUT) == null) {
      captureScreenshot()
      fail("Failed to find loi name popup")
    }
    enterText("An loi name")
    waitClickGone(byText(R.string.save))
  }

  private fun captureScreenshot() {
    val screenShotName = "${javaClass.simpleName}_${nameRule.methodName}"
    Log.d("Screenshots", "Taking screenshot of '$screenShotName'")
    try {
      takeScreenshot().writeToTestStorage(screenShotName)
    } catch (ex: IOException) {
      Log.e("Screenshots", "Could not take the screenshot", ex)
    }
  }
}
