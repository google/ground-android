/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.point

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.job.Style
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import com.google.android.ground.ui.map.CameraPosition
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hamcrest.core.IsNot.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DropPinTaskFragmentTest : BaseTaskFragmentTest<DropPinTaskFragment, DropPinTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory
  @Inject lateinit var localValueStore: LocalValueStore

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.DROP_PIN,
      label = "Task for dropping a pin",
      isRequired = false
    )
  private val job = Job("job", Style("#112233"))

  @Before
  override fun setUp() {
    super.setUp()
    // Disable the instructions dialog to prevent click jacking.
    localValueStore.dropPinInstructionsShown = true
  }

  @Test
  fun `header renders correctly`() {
    setupTaskFragment<DropPinTaskFragment>(job, task)

    hasTaskViewWithoutHeader(task.label)
  }

  @Test
  fun `drop pin button works`() = runWithTestDispatcher {
    val testPosition = CameraPosition(Coordinates(10.0, 20.0))
    setupTaskFragment<DropPinTaskFragment>(job, task)

    viewModel.updateCameraPosition(testPosition)
    onView(withText("Drop pin")).perform(click())

    hasValue(DropPinTaskResult(Point(Coordinates(10.0, 20.0))))
    buttonIsEnabled("Next")
    buttonIsEnabled(ButtonAction.UNDO)
    buttonIsHidden("Drop pin")
  }

  @Test
  fun `info card is hidden`() {
    setupTaskFragment<DropPinTaskFragment>(job, task)

    infoCardHidden()
  }

  @Test
  fun `undo works`() = runWithTestDispatcher {
    val testPosition = CameraPosition(Coordinates(10.0, 20.0))
    setupTaskFragment<DropPinTaskFragment>(job, task)

    viewModel.updateCameraPosition(testPosition)
    onView(withText("Drop pin")).perform(click())
    getButton(ButtonAction.UNDO).performClick()

    hasValue(null)
    buttonIsHidden("Next")
    buttonIsEnabled("Drop pin")
  }

  @Test
  fun `has expected action buttons`() {
    setupTaskFragment<DropPinTaskFragment>(job, task)

    assertFragmentHasButtons(
      ButtonAction.PREVIOUS,
      ButtonAction.SKIP,
      ButtonAction.UNDO,
      ButtonAction.DROP_PIN,
      ButtonAction.NEXT
    )
  }

  @Test
  fun `shows skip when task is optional`() {
    setupTaskFragment<DropPinTaskFragment>(job, task.copy(isRequired = false))

    buttonIsHidden("Next")
    buttonIsEnabled("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Drop pin")
  }

  @Test
  fun `hides skip when task is required`() {
    setupTaskFragment<DropPinTaskFragment>(job, task.copy(isRequired = true))

    buttonIsHidden("Next")
    buttonIsHidden("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Drop pin")
  }
}
