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

@OptIn(ExperimentalTestApi::class)
class SurveySelectorRobot(override val testDriver: TestDriver) : Robot<SurveySelectorRobot>() {
  fun expandPrivateSurveys() {
    testDriver.click(
      TestDriver.Target.Text(
        text = testDriver.getStringResource(R.string.section_shared_with_me),
        substring = true,
      )
    )
  }

  fun selectSurvey(name: String) = testDriver.click(TestDriver.Target.Text(name))
}
