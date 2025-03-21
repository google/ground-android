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
package org.groundplatform.android.ui.datacollection.tasks.instruction

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.BaseTaskFragmentTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class InstructionTaskFragmentTest :
  BaseTaskFragmentTest<InstructionTaskFragment, InstructionTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.INSTRUCTIONS,
      label = "Instruction label",
      isRequired = true,
    )
  private val job = Job("job")

  @Test
  fun testHeader() {
    setupTaskFragment<InstructionTaskFragment>(job, task)
    hasNoTaskViewHeader()
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<InstructionTaskFragment>(job, task)
    assertFragmentHasButtons(ButtonAction.PREVIOUS, ButtonAction.NEXT)
  }

  @Test
  fun `instructions text is displayed`() = runWithTestDispatcher {
    setupTaskFragment<InstructionTaskFragment>(job, task)
    composeTestRule.onNodeWithText("Instruction label").assertIsDisplayed()
  }
}
