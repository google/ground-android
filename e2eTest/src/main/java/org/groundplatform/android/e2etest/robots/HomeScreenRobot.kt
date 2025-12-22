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

import androidx.compose.ui.test.ExperimentalTestApi
import org.groundplatform.android.R
import org.groundplatform.android.e2etest.drivers.TestDriver
import org.groundplatform.android.ui.components.LOCATION_NOT_LOCKED_TEST_TAG

@OptIn(ExperimentalTestApi::class)
class HomeScreenRobot(override val testDriver: TestDriver) : Robot<HomeScreenRobot>() {
  fun moveMap() = testDriver.dragMapBy(500, 500)

  fun recenter() = testDriver.click(TestDriver.Target.TestTag(LOCATION_NOT_LOCKED_TEST_TAG))

  fun addLoi() {
    val contentDescription = testDriver.getStringResource(R.string.add_site)
    testDriver.click(TestDriver.Target.ContentDescription(contentDescription))
  }

  fun selectJob(jobTitle: String) = testDriver.click(TestDriver.Target.Text(jobTitle))

  fun acceptDataSharingTerms() {
    val buttonText = testDriver.getStringResource(R.string.agree_checkbox)
    testDriver.click(TestDriver.Target.Text(buttonText))
  }

  fun deleteLoi(name: String) {
    testDriver.clickMapMarker(name)
    testDriver.click(TestDriver.Target.Text(testDriver.getStringResource(R.string.delete_site)))
    testDriver.click(TestDriver.Target.Text(testDriver.getStringResource(R.string.delete)))
  }
}
