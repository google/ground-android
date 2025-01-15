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
package com.google.android.ground.ui.datacollection.tasks.photo

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.job.Style
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PhotoTaskFragmentTest : BaseTaskFragmentTest<PhotoTaskFragment, PhotoTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.PHOTO,
      label = "Task for capturing a photo",
      isRequired = false,
    )
  private val job = Job("job", Style("#112233"))

  override fun setUp() {
    super.setUp()

    whenever(dataCollectionViewModel.surveyId).thenReturn("test survey id")
  }

  @Test
  fun testHeader() {
    setupTaskFragment<PhotoTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<PhotoTaskFragment>(job, task)

    assertFragmentHasButtons(
      ButtonAction.PREVIOUS,
      ButtonAction.UNDO,
      ButtonAction.SKIP,
      ButtonAction.NEXT,
    )
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<PhotoTaskFragment>(job, task.copy(isRequired = false))

    runner()
      .assertButtonIsDisabled("Next")
      .assertButtonIsEnabled("Skip")
      .assertButtonIsHidden("Undo", true)
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<PhotoTaskFragment>(job, task.copy(isRequired = true))

    runner()
      .assertButtonIsDisabled("Next")
      .assertButtonIsHidden("Skip")
      .assertButtonIsHidden("Undo", true)
  }
}
