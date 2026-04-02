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

package org.groundplatform.android.ui.home.mapcontainer

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.ADHOC_JOB
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST_FEATURE
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.di.LocationOfInterestRepositoryModule
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentState
import org.groundplatform.android.ui.home.mapcontainer.jobs.SelectedLoiSheetData
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@UninstallModules(LocationOfInterestRepositoryModule::class)
@RunWith(RobolectricTestRunner::class)
class HomeScreenMapContainerViewModelTest : BaseHiltTest() {
  @Inject lateinit var viewModel: HomeScreenMapContainerViewModel
  @Inject lateinit var authenticationManager: FakeAuthenticationManager
  @Inject lateinit var remoteDataStore: FakeRemoteDataStore
  @Inject lateinit var userRepository: UserRepositoryInterface
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @BindValue @Mock lateinit var loiRepository: LocationOfInterestRepositoryInterface

  @Before
  override fun setUp() {
    super.setUp()
    runWithTestDispatcher {
      // Setup user
      authenticationManager.setUser(USER)
      userRepository.saveUserDetails(USER)

      // Setup survey and LOIs
      remoteDataStore.surveys = listOf(SURVEY)
      remoteDataStore.predefinedLois = listOf(LOCATION_OF_INTEREST)
      activateSurvey(SURVEY.id)
      advanceUntilIdle()
      `when`(loiRepository.getWithinBounds(SURVEY, BOUNDS))
        .thenReturn(flowOf(listOf(LOCATION_OF_INTEREST)))
      viewModel.onMapCameraMoved(CAMERA_POSITION)
      advanceUntilIdle()
    }
  }

  @Test
  fun `renders the job card when zoomed into LOI and clicked on`() = runWithTestDispatcher {
    viewModel.onFeatureClicked(features = setOf(LOCATION_OF_INTEREST_FEATURE))
    val state = viewModel.processJobMapComponentState().first()
    assertThat(state)
      .isEqualTo(
        JobMapComponentState.LoiSelected(
          SelectedLoiSheetData(canCollectData = true, LOCATION_OF_INTEREST, 0, true)
        )
      )
  }

  @Test
  fun `deleteLoi deletes the loi and deselects it`() = runWithTestDispatcher {
    viewModel.onFeatureClicked(setOf(LOCATION_OF_INTEREST_FEATURE))
    assertThat(viewModel.featureClicked.value).isNotNull()

    viewModel.deleteLoi(LOCATION_OF_INTEREST)
    advanceUntilIdle()

    verify(loiRepository).deleteLoi(LOCATION_OF_INTEREST)
    assertThat(viewModel.featureClicked.value).isNull()
  }

  @Test
  fun `job component state is JobSelectionModal when multiple jobs exist`() =
    runWithTestDispatcher {
      val state =
        JobMapComponentState.AddLoiButton(
          listOf(
            AdHocDataCollectionButtonData(canCollectData = true, job = ADHOC_JOB),
            AdHocDataCollectionButtonData(
              canCollectData = true,
              job = ADHOC_JOB.copy(id = "ADHOC_JOB_2", name = "Adhoc Job 2"),
            ),
          )
        )

      val result = viewModel.resolveAddLoiAction(state)

      assertThat(result).isNull()
      val updatedState = viewModel.processJobMapComponentState().first()
      assertThat(updatedState).isInstanceOf(JobMapComponentState.JobSelectionModal::class.java)
    }

  @Test
  fun `job component state is AddLoiButton when there are adhoc jobs and modal is not shown`() =
    runWithTestDispatcher {
      val state = viewModel.processJobMapComponentState().first()
      assertThat(state).isInstanceOf(JobMapComponentState.AddLoiButton::class.java)
      val addLoiState = state as JobMapComponentState.AddLoiButton
      assertThat(addLoiState.jobs.map { it.job }).containsExactly(ADHOC_JOB)
    }

  @Test
  fun `setJobSelectionModalVisibility hides map actions when modal is shown`() =
    runWithTestDispatcher {
      viewModel.setJobSelectionModalVisibility(true)

      assertThat(viewModel.shouldShowMapActions.value).isFalse()
    }

  @Test
  fun `setJobSelectionModalVisibility restores map actions when modal is hidden`() =
    runWithTestDispatcher {
      viewModel.setJobSelectionModalVisibility(true)
      viewModel.setJobSelectionModalVisibility(false)

      assertThat(viewModel.shouldShowMapActions.value).isTrue()
    }

  @Test
  fun `resolveAddLoiAction returns single job when only one exists`() = runWithTestDispatcher {
    val state = viewModel.processJobMapComponentState().first()
    assertThat(state).isInstanceOf(JobMapComponentState.AddLoiButton::class.java)

    val result = viewModel.resolveAddLoiAction(state)

    assertThat(result).isNotNull()
    assertThat(result!!.job).isEqualTo(ADHOC_JOB)
    // Modal should not be shown for a single job.
    val updatedState = viewModel.processJobMapComponentState().first()
    assertThat(updatedState).isInstanceOf(JobMapComponentState.AddLoiButton::class.java)
  }

  companion object {
    private val BOUNDS = Bounds(Coordinates(-20.0, -20.0), Coordinates(-10.0, -10.0))
    val CAMERA_POSITION =
      CameraPosition(
        coordinates = LOCATION_OF_INTEREST.geometry.center(),
        zoomLevel = 16f,
        bounds = BOUNDS,
      )
  }
}
