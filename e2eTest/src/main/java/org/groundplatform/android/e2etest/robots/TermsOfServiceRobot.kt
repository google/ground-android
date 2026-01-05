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
import org.groundplatform.android.e2etest.drivers.TestDriver

class TermsOfServiceRobot(override val testDriver: TestDriver) : Robot<TermsOfServiceRobot>() {
  fun agree(): TermsOfServiceRobot {
    with(TestDriver.Target.Text(testDriver.getStringResource(R.string.agree_checkbox))) {
      testDriver.scrollTo(this)
      testDriver.click(this)
    }
    with(TestDriver.Target.Text(testDriver.getStringResource(R.string.agree_terms))) {
      testDriver.scrollTo(this)
      testDriver.click(this)
    }
    return this
  }
}
