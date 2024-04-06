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
import com.google.android.ground.e2etest.TestConfig.TEST_SURVEY_TASKS_ADHOC
import com.google.android.ground.model.task.Task
import junit.framework.TestCase.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SurveyRunnerTest : AutomatorRunner {

  override lateinit var device: UiDevice

  private val selectSurveyTitle = stringResource(R.string.select_survey_title)
  private val collectData = stringResource(R.string.collect_data)
  private val newDataCollectionSite = stringResource(R.string.new_site)
  private val close = stringResource(R.string.close)
  private val dropPin = stringResource(R.string.drop_pin)
  private val addPoint = stringResource(R.string.add_point)
  private val complete = stringResource(R.string.complete_polygon)
  private val capture = stringResource(R.string.capture)
  private val camera = stringResource(R.string.camera)
  private val ok = stringResource(R.string.ok)
  private val next = stringResource(R.string.next)
  private val done = stringResource(R.string.done)
  private val save = stringResource(R.string.save)

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
    fillOutTaskData(isAdHoc = false, TEST_SURVEY_TASKS_ADHOC.drop(1))
    clickSubmissionConfirmationDone()
  }

  private fun launchGround() {
    // Initialize UiDevice instance.
    device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    launchPackage(GROUND_PACKAGE)

    // Wait for the app to appear.
    if (!device.wait(Until.hasObject(By.pkg(GROUND_PACKAGE).depth(0)), LONG_TIMEOUT)) {
      fail("Failed to launch app.")
    }
  }

  private fun signIn() {
    if (!waitClickGone(By.clazz("android.widget.Button"), LONG_TIMEOUT)) {
      fail("Failed to sign in.")
    }
  }

  private fun selectTestSurvey() {
    device.wait(Until.hasObject(By.text(selectSurveyTitle)), LONG_TIMEOUT)
    val testSurveySelector =
      By.clazz("androidx.cardview.widget.CardView")
        .hasDescendant(By.clazz("android.widget.TextView").textContains(TEST_SURVEY_IDENTIFIER))
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
      By.clazz("androidx.cardview.widget.CardView").hasDescendant(By.text(collectData))
    if (device.wait(Until.hasObject(dataCollectionCardSelector), LONG_TIMEOUT) == null) {
      fail("Failed to zoom in to location.")
    }
  }

  private fun startAdHocLoiTask() {
    val dataCollectionCardSelector = By.clazz("androidx.cardview.widget.CardView")
    device.wait(Until.hasObject(dataCollectionCardSelector), LONG_TIMEOUT)
    val cards = device.findObjects(dataCollectionCardSelector)
    cards.forEach { it.swipe(Direction.LEFT, 1F) }
    val loiCollectDataButtonSelector =
      By.text(collectData)
        .hasAncestor(dataCollectionCardSelector.hasDescendant(By.text(newDataCollectionSite)))
    if (!waitClickGone(loiCollectDataButtonSelector)) {
      fail("Failed to start ad-hoc loi data collection.")
    }
  }

  private fun startPredefinedLoiTask() {
    val dataCollectionCardSelector = By.clazz("androidx.cardview.widget.CardView")
    device.wait(Until.hasObject(dataCollectionCardSelector), LONG_TIMEOUT)
    // Assume that the first card is the predefined LOI.
    val loiCollectDataButtonSelector = By.text(collectData)
    if (!waitClickGone(loiCollectDataButtonSelector)) {
      fail("Failed to start predefined loi data collection.")
    }
  }

  private fun fillOutTaskData(isAdHoc: Boolean, taskList: List<Task.Type>) {
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
    waitClickGone(By.text(close))
    waitClickGone(By.text(dropPin))
  }

  private fun completeDrawArea() {
    // Instructions dialog may be triggered.
    waitClickGone(By.text(close))
    waitClickGone(By.text(addPoint))
    waitClickGone(By.text(addPoint))
    waitClickGone(By.text(addPoint))
    waitClickGone(By.text(complete))
  }

  private fun completeCaptureLocation() {
    waitClickGone(By.text(capture))
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
    waitClickGone(By.text(camera))
    waitClickGone(By.res("com.android.camera2:id/shutter_button"))
    waitClickGone(By.res("com.android.camera2:id/done_button"))
  }

  private fun completeNumber() {
    enterText("1234")
  }

  private fun completeDate() {
    device.findObject(By.clazz("android.widget.EditText")).click()
    waitClickGone(By.text(ok))
  }

  private fun completeTime() {
    device.findObject(By.clazz("android.widget.EditText")).click()
    waitClickGone(By.text(ok))
  }

  private fun clickNext() {
    waitClickGone(By.text(next))
  }

  private fun clickDone() {
    waitClickGone(By.text(done))
  }

  private fun clickSubmissionConfirmationDone() {
    waitClickGone(By.text(done), LONG_TIMEOUT)
  }

  private fun clickLocationLock() {
    waitClickGone(By.res("com.google.android.ground", "location_lock_btn"), timeout = LONG_TIMEOUT)
  }

  private fun setLoiName() {
    device.wait(Until.hasObject(By.text(save)), SHORT_TIMEOUT)
    enterText("An loi name")
    waitClickGone(By.text(save))
  }
}
