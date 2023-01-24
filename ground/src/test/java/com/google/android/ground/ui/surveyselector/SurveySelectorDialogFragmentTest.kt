/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.surveyselector

import android.os.Looper
import android.view.View
import android.widget.ListView
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.MainActivity
import com.google.android.ground.R
import com.google.android.ground.coroutines.DefaultDispatcher
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalDataStoreModule
import com.google.android.ground.persistence.local.room.RoomLocalDataStore
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestMutationStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.safeEq
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.reactivex.Maybe
import java8.util.Optional
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@UninstallModules(LocalDataStoreModule::class)
class SurveySelectorDialogFragmentTest : BaseHiltTest() {
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject @DefaultDispatcher lateinit var testDispatcher: CoroutineDispatcher

  @BindValue @InjectMocks var mockLocalDataStore: LocalDataStore = RoomLocalDataStore()

  @BindValue @Mock lateinit var mockSurveyStore: LocalSurveyStore
  @BindValue @Mock lateinit var mockUserStore: LocalUserStore
  @BindValue @Mock lateinit var localLocationOfInterestStore: LocalLocationOfInterestMutationStore

  private lateinit var surveySelectorDialogFragment: SurveySelectorDialogFragment

  @Before
  override fun setUp() {
    super.setUp()
    fakeRemoteDataStore.setTestSurveys(ImmutableList.of(TEST_SURVEY_1, TEST_SURVEY_2))
    fakeAuthenticationManager.setUser(FakeData.USER)
    setUpFragment()
  }

  private fun setUpFragment() {
    val activityController = Robolectric.buildActivity(MainActivity::class.java)
    val activity = activityController.setup().get()

    surveySelectorDialogFragment = SurveySelectorDialogFragment()

    surveySelectorDialogFragment.showNow(
      activity.supportFragmentManager,
      SurveySelectorDialogFragment::class.java.simpleName
    )
    shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun show_surveyDialogIsShown() {
    val listView = surveySelectorDialogFragment.dialog!!.currentFocus as ListView

    assertThat(listView.visibility).isEqualTo(View.VISIBLE)
    assertThat(listView.findViewById<View>(R.id.survey_name)?.visibility).isEqualTo(View.VISIBLE)
  }

  @Test
  fun show_surveySelected_surveyIsActivated() =
    runTest(testDispatcher) {
      val listView = surveySelectorDialogFragment.dialog!!.currentFocus as ListView

      // TODO: Replace mocks with inserting the survey in local db
      Mockito.`when`(mockLocalDataStore.surveyStore.getSurveyById(safeEq(TEST_SURVEY_2.id)))
        .thenReturn(Maybe.just(TEST_SURVEY_2))
      shadowOf(listView).performItemClick(1)
      shadowOf(Looper.getMainLooper()).idle()
      advanceUntilIdle()

      // Verify dialog is dismissed.
      assertThat(surveySelectorDialogFragment.dialog).isNull()
      surveyRepository.activeSurvey.test().assertValue(Optional.of(TEST_SURVEY_2))
    }

  companion object {
    private val TEST_SURVEY_1 = FakeData.SURVEY.copy(id = "some id 1")
    private val TEST_SURVEY_2 = FakeData.SURVEY.copy(id = "some id 2")
  }
}
