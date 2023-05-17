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
package com.google.android.ground.ui.datacollection.tasks.polygon

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PolygonDrawingTaskFragmentTest :
  BaseTaskFragmentTest<PolygonDrawingTaskFragment, PolygonDrawingViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.DRAW_POLYGON,
      label = "Task for drawing a polygon",
      isRequired = false
    )

  @Test
  fun testHeader() {
    setupTaskFragment<PolygonDrawingTaskFragment>(task)

    hasTaskViewWithoutHeader("Draw an area")
  }

  @Test
  fun testHeader_click_displaysTaskLabel() = runWithTestDispatcher {
    setupTaskFragment<PolygonDrawingTaskFragment>(task)

    onView(withText("Draw an area")).perform(click())

    onView(withText("Task for drawing a polygon")).check(matches(isDisplayed()))
  }

  @Test
  fun testInfoCard_noTaskData() {
    setupTaskFragment<PolygonDrawingTaskFragment>(task)

    infoCardHidden()
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<PolygonDrawingTaskFragment>(task)

    hasButtons(
      ButtonAction.CONTINUE,
      ButtonAction.SKIP,
      ButtonAction.UNDO,
      ButtonAction.ADD_POINT,
      ButtonAction.COMPLETE
    )
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<PolygonDrawingTaskFragment>(task.copy(isRequired = false))

    buttonIsHidden("Continue")
    buttonIsEnabled("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Add point")
    buttonIsHidden("Complete")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<PolygonDrawingTaskFragment>(task.copy(isRequired = true))

    buttonIsHidden("Continue")
    buttonIsHidden("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Add point")
    buttonIsHidden("Complete")
  }
}
