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

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.ADHOC_JOB
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST_FEATURE
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.R
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.map.Bounds
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.DataCollectionEntryPointData
import org.groundplatform.android.ui.home.mapcontainer.jobs.SelectedLoiSheetData
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenMapContainerViewModelTest : BaseHiltTest() {
  @Inject lateinit var viewModel: HomeScreenMapContainerViewModel
  @BindValue
  val surveyRepository: SurveyRepository = org.mockito.Mockito.mock(SurveyRepository::class.java)
  @Inject lateinit var authenticationManager: FakeAuthenticationManager
  @Inject lateinit var remoteDataStore: FakeRemoteDataStore
  @Inject lateinit var userRepository: UserRepository
  @BindValue
  val activateSurvey: ActivateSurveyUseCase =
    org.mockito.Mockito.mock(ActivateSurveyUseCase::class.java)
  @BindValue
  val mapStateRepository: MapStateRepository =
    org.mockito.Mockito.mock(MapStateRepository::class.java)
  @BindValue
  val localValueStore: LocalValueStore = org.mockito.Mockito.mock(LocalValueStore::class.java)
  @BindValue
  val loiRepository: LocationOfInterestRepository =
    org.mockito.Mockito.mock(LocationOfInterestRepository::class.java)

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  @Before
  override fun setUp() {
    val localTestDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
    Dispatchers.setMain(localTestDispatcher)
    val context =
      androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    val config =
      androidx.work.Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.DEBUG)
        .setExecutor(androidx.work.testing.SynchronousExecutor())
        .build()
    androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

    whenever(surveyRepository.activeSurveyFlow)
      .thenReturn(kotlinx.coroutines.flow.MutableStateFlow(SURVEY))
    whenever(surveyRepository.activeSurvey).thenReturn(SURVEY)

    whenever(mapStateRepository.mapTypeFlow)
      .thenReturn(
        kotlinx.coroutines.flow.MutableStateFlow(
          org.groundplatform.android.model.map.MapType.TERRAIN
        )
      )
    whenever(mapStateRepository.offlineImageryEnabledFlow)
      .thenReturn(kotlinx.coroutines.flow.MutableStateFlow(false))
    whenever(mapStateRepository.isLocationLockEnabled).thenReturn(false)

    MockitoAnnotations.openMocks(this)
    super.setUp()
    // Overwrite the injected testDispatcher with our local one to ensure consistency
    testDispatcher = localTestDispatcher

    runWithTestDispatcher {
      // Setup user
      authenticationManager.setUser(USER)
      authenticationManager.signIn()
      userRepository.saveUserDetails(USER)
      // Spy userRepository to prevent actual survey role checks if needed, but if sign-in works, it might be fine. 
      // Reverting spy for now to test if signIn() fixes it.
      // userRepository = org.mockito.Mockito.spy(userRepository)
      // org.mockito.kotlin.doReturn(true).whenever(userRepository).canUserSubmitData()

      // Setup survey and LOIs
      remoteDataStore.surveys = listOf(SURVEY)
      remoteDataStore.predefinedLois = listOf(LOCATION_OF_INTEREST)
      advanceUntilIdle()
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      `when`(loiRepository.getWithinBounds(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
        .thenReturn(flow { 
            emit(listOf(LOCATION_OF_INTEREST)) 
            awaitCancellation() 
        })
      viewModel.onMapCameraMoved(CAMERA_POSITION)
      advanceUntilIdle()
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      advanceUntilIdle()
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }
  }

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  @org.junit.After
  fun tearDown() {
    viewModel.viewModelScope.cancel()
  }

  @Test
  fun `renders the job card when zoomed into LOI and clicked on`() = runWithTestDispatcher {
    val job = launch {
      viewModel.jobMapComponentState.collect()
    }
    viewModel.onFeatureClicked(features = setOf(LOCATION_OF_INTEREST_FEATURE))
    advanceUntilIdle()
    
    val state = viewModel.jobMapComponentState.value
    assertThat(state.selectedLoi)
      .isEqualTo(SelectedLoiSheetData(canCollectData = true, LOCATION_OF_INTEREST, 0, true))
    assertThat(state.adHocDataCollectionButtonData)
      .isEqualTo(listOf(AdHocDataCollectionButtonData(canCollectData = true, ADHOC_JOB)))
      
    job.cancel()
  }

  @Test
  fun `queueDataCollection shows terms dialog when consent not given`() = runWithTestDispatcher {
    whenever(localValueStore.getDataSharingConsent(anyString())).thenReturn(false)
    val buttonData = AdHocDataCollectionButtonData(canCollectData = true, ADHOC_JOB)

    viewModel.queueDataCollection(buttonData)
    advanceUntilIdle()

    assertThat(viewModel.dataSharingTerms.value).isEqualTo(SURVEY.dataSharingTerms)
  }

  @Test
  fun `queueDataCollection navigates when consent already given`() = runWithTestDispatcher {
    whenever(localValueStore.getDataSharingConsent(anyString())).thenReturn(true)
    val buttonData = AdHocDataCollectionButtonData(canCollectData = true, ADHOC_JOB)
    val navigationEvents = mutableListOf<DataCollectionEntryPointData>()
    // We launch a collector for the SharedFlow
    val job = launch {
      viewModel.navigateToDataCollectionFragment.collect { navigationEvents.add(it) }
    }
    advanceUntilIdle()

    viewModel.queueDataCollection(buttonData)
    advanceUntilIdle()
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    advanceUntilIdle()

    assertThat(viewModel.dataSharingTerms.value).isNull()
    assertThat(navigationEvents).contains(buttonData)
    job.cancel()
  }

  @Test
  fun `onTermsConsentGiven updates consent and navigates`() = runWithTestDispatcher {
    whenever(localValueStore.getDataSharingConsent(anyString())).thenReturn(false)
    val buttonData = AdHocDataCollectionButtonData(canCollectData = true, ADHOC_JOB)
    val navigationEvents = mutableListOf<DataCollectionEntryPointData>()
    val job = launch {
      viewModel.navigateToDataCollectionFragment.collect { navigationEvents.add(it) }
    }

    viewModel.queueDataCollection(buttonData)
    advanceUntilIdle()

    // TERMS shown
    assertThat(viewModel.dataSharingTerms.value).isNotNull()

    viewModel.onTermsConsentGiven()
    advanceUntilIdle()
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    advanceUntilIdle()

    // Dialog hidden, navigation happened, consent saved
    assertThat(viewModel.dataSharingTerms.value).isNull()
    assertThat(navigationEvents).contains(buttonData)
    verify(localValueStore).setDataSharingConsent(SURVEY.id, true)
    job.cancel()
  }

  @Test
  fun `queueDataCollection emits error when terms are invalid`() = runWithTestDispatcher {
    whenever(localValueStore.getDataSharingConsent(anyString())).thenReturn(false)
    val buttonData = AdHocDataCollectionButtonData(canCollectData = true, ADHOC_JOB)

    // Create survey with invalid terms (custom type but empty text)
    // Use unique ID to ensure we don't use cached survey from setUp
    val invalidTerms =
      org.groundplatform.android.proto.Survey.DataSharingTerms.newBuilder()
        .setType(org.groundplatform.android.proto.Survey.DataSharingTerms.Type.CUSTOM)
        .setCustomText("")
        .build()
    val invalidSurvey = SURVEY.copy(id = "invalid_survey_id", dataSharingTerms = invalidTerms)
    remoteDataStore.surveys = listOf(invalidSurvey)
    whenever(surveyRepository.activeSurvey).thenReturn(invalidSurvey)
    advanceUntilIdle()

    val errorEvents = mutableListOf<Int>()
    val job = launch { viewModel.termsError.collect { errorEvents.add(it) } }

    viewModel.queueDataCollection(buttonData)
    advanceUntilIdle()

    assertThat(errorEvents).contains(R.string.invalid_data_sharing_terms)
    job.cancel()
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
