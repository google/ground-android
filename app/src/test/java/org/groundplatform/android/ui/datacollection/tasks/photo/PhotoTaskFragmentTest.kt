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
package org.groundplatform.android.ui.datacollection.tasks.photo

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.Fragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltTestApplication
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.groundplatform.android.ui.home.HomeScreenViewModel
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PhotoTaskFragmentTest {

//  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
//  @get:Rule(order = 1) var composeTestRule = createAndroidComposeRule<MainActivity>()
//
//  @BindValue @Mock lateinit var dataCollectionViewModel: DataCollectionViewModel
//  @BindValue @Mock lateinit var userMediaRepository: UserMediaRepository
//  @BindValue @Mock lateinit var permissionsManager: PermissionsManager
//  @BindValue @Mock lateinit var popups: EphemeralPopups
//  @BindValue @Mock lateinit var viewModelFactory: ViewModelFactory
//
//  private val task =
//    Task(
//      id = "task_1",
//      index = 0,
//      type = Task.Type.PHOTO,
//      label = "Task for capturing a photo",
//      isRequired = false,
//    )
//
//  private val homeScreenViewModel: HomeScreenViewModel = mock()
//  private lateinit var viewModel: PhotoTaskViewModel
//
//  @Before
//  fun setup() {
//    hiltRule.inject()
//    viewModel = PhotoTaskViewModel(userMediaRepository)
//
//    whenever(viewModelFactory.create(PhotoTaskViewModel::class.java)) doReturn viewModel
//    whenever(dataCollectionViewModel.getTaskViewModel(task)) doReturn viewModel
//    whenever(dataCollectionViewModel.requireSurveyId()) doReturn "test survey id"
//
//    runBlocking {
//      val file = File(RuntimeEnvironment.getApplication().filesDir, "image.jpg")
//      file.createNewFile()
//      whenever(userMediaRepository.createImageFile(any())) doReturn file
//      whenever(userMediaRepository.getUriForFile(any())) doReturn Uri.fromFile(file)
//    }
//  }
//
//  private fun setupScreen(task: Task = this.task) {
//    viewModel.initialize(task)
//    composeTestRule.setContent {
//      PhotoTaskScreen(viewModel, TaskScreenEnvironment(dataCollectionViewModel, homeScreenViewModel))
//    }
//  }
//
//  @Test
//  fun `displays task header correctly`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//    composeTestRule.onNodeWithText(task.label).assertIsDisplayed()
//  }
//
//  @Test
//  fun `Initial action buttons state`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.undo_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.skip_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//  }
//
//  @Test
//  fun `Initial action buttons state when task is required`() = runTest {
//    setupScreen(task.copy(isRequired = true))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.undo_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.skip_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//  }
//
//  @Test
//  fun `taking photo sends intent`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    composeTestRule.onNodeWithText("Camera").performClick()
//    ShadowLooper.idleMainLooper()
//
//    verify(userMediaRepository).createImageFile(any())
//    verify(userMediaRepository).getUriForFile(any())
//    assertThat(viewModel.hasLaunchedCamera).isTrue()
//  }
}
