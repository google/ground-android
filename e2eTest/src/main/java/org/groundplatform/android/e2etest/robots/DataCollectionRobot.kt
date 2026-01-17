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
package org.groundplatform.android.e2etest.robots

import org.groundplatform.android.R
import org.groundplatform.android.e2etest.TestConfig.LOI_NAME
import org.groundplatform.android.e2etest.TestTask
import org.groundplatform.android.e2etest.drivers.TestDriver
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.components.LOI_NAME_TEXT_FIELD_TEST_TAG
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.OTHER_INPUT_TEXT_TEST_TAG
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.SELECT_MULTIPLE_CHECKBOX_TEST_TAG
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.SELECT_MULTIPLE_RADIO_TEST_TAG
import org.groundplatform.android.ui.datacollection.tasks.number.INPUT_NUMBER_TEST_TAG
import org.groundplatform.android.ui.datacollection.tasks.text.INPUT_TEXT_TEST_TAG

class DataCollectionRobot(override val testDriver: TestDriver) : Robot<DataCollectionRobot>() {
  fun dismissInstructions() {
    val buttonText = testDriver.getStringResource(R.string.close)
    testDriver.click(TestDriver.Target.Text(buttonText))
  }

  fun runTasks(taskList: List<TestTask>) {
    taskList.forEach { task ->
      when (task.taskType) {
        Task.Type.UNKNOWN ->
          throw IllegalStateException(
            "Something is wrong with the tasks defined in the Firebase emulator"
          )
        Task.Type.TEXT -> textTask()
        Task.Type.MULTIPLE_CHOICE -> multipleChoiceTask(task.selectIndexes!!)
        Task.Type.PHOTO -> cameraTask()
        Task.Type.NUMBER -> numberTask()
        Task.Type.DATE -> dateTask()
        Task.Type.TIME -> timeTask()
        Task.Type.DROP_PIN -> dropPinTask()
        Task.Type.DRAW_AREA -> drawAreaTask()
        Task.Type.CAPTURE_LOCATION -> captureLocationTask()
        Task.Type.INSTRUCTIONS -> {
          /* Nothing to do, just read */
        }
      }

      if (task == taskList.last()) {
        testDriver.click(TestDriver.Target.Text(testDriver.getStringResource(R.string.done)))
      } else {
        testDriver.click(TestDriver.Target.Text(testDriver.getStringResource(R.string.next)))
      }

      if (task.taskType == Task.Type.DRAW_AREA || task.taskType == Task.Type.DROP_PIN) {
        nameLocation()
      }
    }
    testDriver.click(TestDriver.Target.Text(testDriver.getStringResource(R.string.close)))
  }

  private fun drawAreaTask(): DataCollectionRobot {
    val points = listOf((0 to 0), (-500 to 0), (0 to -500), (500 to 0), (0 to 500))
    val nextButton = TestDriver.Target.Text(testDriver.getStringResource(R.string.add_point))
    val completeButton =
      TestDriver.Target.Text(testDriver.getStringResource(R.string.complete_polygon))
    points.forEachIndexed { index, point ->
      when (index) {
        0 -> testDriver.click(nextButton)
        points.lastIndex -> {
          testDriver.dragMapBy(point.first, point.second)
          testDriver.click(completeButton)
        }
        else -> {
          testDriver.dragMapBy(point.first, point.second)
          testDriver.click(nextButton)
        }
      }
    }
    return this
  }

  private fun dropPinTask(): DataCollectionRobot {
    testDriver.click(TestDriver.Target.Text(testDriver.getStringResource(R.string.drop_pin)))
    return this
  }

  private fun textTask() {
    testDriver.insertText("Test", TestDriver.Target.TestTag(INPUT_TEXT_TEST_TAG))
  }

  private fun numberTask() {
    testDriver.insertText("2025", TestDriver.Target.TestTag(INPUT_NUMBER_TEST_TAG))
  }

  private fun nameLocation() {
    testDriver.insertText(
      text = LOI_NAME,
      target = TestDriver.Target.TestTag(LOI_NAME_TEXT_FIELD_TEST_TAG),
    )
    testDriver.click(TestDriver.Target.Text(testDriver.getStringResource(R.string.save)))
  }

  private fun multipleChoiceTask(selectIndexes: List<Int>) {
    if (selectIndexes.size == 1) {
      testDriver.selectFromList(
        TestDriver.Target.TestTag(SELECT_MULTIPLE_RADIO_TEST_TAG),
        selectIndexes[0],
      )
    } else {
      selectIndexes.forEach {
        testDriver.selectFromList(TestDriver.Target.TestTag(SELECT_MULTIPLE_CHECKBOX_TEST_TAG), it)
      }
      testDriver.insertText("Other", TestDriver.Target.TestTag(OTHER_INPUT_TEXT_TEST_TAG))
    }
  }

  private fun cameraTask() {
    testDriver.click(TestDriver.Target.ViewId(R.id.btn_camera))
    testDriver.takePhoto()
  }

  private fun dateTask() {
    testDriver.setDate()
  }

  private fun timeTask() {
    testDriver.setTime()
  }

  private fun captureLocationTask() {
    testDriver.click(TestDriver.Target.Text(testDriver.getStringResource(R.string.capture)))
  }
}
