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
package org.groundplatform.android.e2etest.extensions

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import org.groundplatform.android.e2etest.drivers.TestDriver

fun AndroidComposeTestRule<*, *>.onTarget(
  target: TestDriver.Target,
  index: Int = 0,
): SemanticsNodeInteraction =
  when (target) {
    is TestDriver.Target.ContentDescription -> onAllNodesWithContentDescription(target.text)[index]
    is TestDriver.Target.TestTag -> onAllNodesWithTag(target.tag)[index]
    is TestDriver.Target.Text -> onAllNodesWithText(target.text, target.substring)[index]
    else -> throw IllegalArgumentException("Not a Compose node")
  }
