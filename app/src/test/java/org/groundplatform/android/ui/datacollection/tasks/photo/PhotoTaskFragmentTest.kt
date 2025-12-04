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

import androidx.fragment.app.Fragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.BaseTaskFragmentTest
import org.groundplatform.android.ui.home.HomeScreenViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PhotoTaskFragmentTest : BaseTaskFragmentTest<PhotoTaskFragment, PhotoTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @BindValue @Mock lateinit var userMediaRepository: UserMediaRepository
  @BindValue @Mock override lateinit var viewModelFactory: ViewModelFactory
  @BindValue @Mock lateinit var permissionsManager: PermissionsManager
  @BindValue @Mock lateinit var popups: EphemeralPopups

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.PHOTO,
      label = "Task for capturing a photo",
      isRequired = false,
    )
  private val job = Job("job", Style("#112233"))

  @Mock lateinit var homeScreenViewModel: HomeScreenViewModel
  lateinit var photoTaskViewModel: PhotoTaskViewModel

  override fun setUp() {
    super.setUp()
    homeScreenViewModel = org.mockito.Mockito.mock(HomeScreenViewModel::class.java)
    photoTaskViewModel = PhotoTaskViewModel(userMediaRepository)

    doReturn(homeScreenViewModel).`when`(viewModelFactory).create(HomeScreenViewModel::class.java)
    doReturn(photoTaskViewModel).`when`(viewModelFactory).create(PhotoTaskViewModel::class.java)
    doReturn(homeScreenViewModel)
      .`when`(viewModelFactory)
      .get(any<Fragment>(), eq(HomeScreenViewModel::class.java))
    whenever(dataCollectionViewModel.requireSurveyId()).thenReturn("test survey id")
    kotlinx.coroutines.runBlocking {
      val file =
        java.io.File(
          org.robolectric.RuntimeEnvironment.getApplication()
            .getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
          "image.jpg",
        )
      file.createNewFile()
      whenever(userMediaRepository.createImageFile(any())).thenReturn(file)
      whenever(userMediaRepository.getUriForFile(any())).thenReturn(android.net.Uri.EMPTY)
    }
  }

  @Test
  fun `displays task header correctly`() {
    setupTaskFragment<PhotoTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun `action buttons`() {
    setupTaskFragment<PhotoTaskFragment>(job, task)

    assertFragmentHasButtons(
      ButtonAction.PREVIOUS,
      ButtonAction.UNDO,
      ButtonAction.SKIP,
      ButtonAction.NEXT,
    )
  }

  @Test
  fun `action buttons when task is optional`() {
    setupTaskFragment<PhotoTaskFragment>(job, task.copy(isRequired = false))

    runner()
      .assertButtonIsDisabled("Next")
      .assertButtonIsEnabled("Skip")
      .assertButtonIsHidden("Undo", true)
  }

  @Test
  fun `action buttons when task is required`() {
    setupTaskFragment<PhotoTaskFragment>(job, task.copy(isRequired = true))

    runner()
      .assertButtonIsDisabled("Next")
      .assertButtonIsHidden("Skip")
      .assertButtonIsHidden("Undo", true)
  }

  @Test
  fun `taking photo sends intent`() {
    setupTaskFragment<PhotoTaskFragment>(job, task)

    onView(withId(R.id.btn_camera)).perform(click())

    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    kotlinx.coroutines.runBlocking {
      verify(userMediaRepository).createImageFile(any())
      verify(userMediaRepository).getUriForFile(any())
    }

    assertThat(photoTaskViewModel.hasLaunchedCamera).isTrue()
  }
}
