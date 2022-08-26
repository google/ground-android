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
import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalDataStoreModule
import com.google.android.ground.repository.SurveyRepository
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.FakeData.USER
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.reactivex.Maybe
import java8.util.Optional
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@UninstallModules(LocalDataStoreModule::class)
class SurveySelectorDialogFragmentTest : BaseHiltTest() {
    @Inject
    lateinit var surveyRepository: SurveyRepository

    @Inject
    lateinit var fakeRemoteDataStore: FakeRemoteDataStore

    @BindValue
    @Mock
    lateinit var mockLocalDataStore: LocalDataStore

    private lateinit var surveySelectorDialogFragment: SurveySelectorDialogFragment

    private val survey1 = Survey(
        "1",
        "Survey title",
        "Test survey description",
        ImmutableMap.of(),
        ImmutableList.of(),
        ImmutableMap.of(USER.email, "data_collector")
    )

    private val survey2 = Survey(
        "2",
        "Survey title",
        "Test survey description",
        ImmutableMap.of(),
        ImmutableList.of(),
        ImmutableMap.of(USER.email, "data_collector")
    )
    private val surveys: List<Survey> = ImmutableList.of(survey1, survey2)

    @Before
    override fun setUp() {
        super.setUp()
        fakeRemoteDataStore.setTestSurveys(surveys)
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
        val listView = surveySelectorDialogFragment.dialog?.currentFocus as ListView?

        assertThat(listView).isNotNull()
        assertThat(listView?.visibility).isEqualTo(View.VISIBLE)
        assertThat(listView?.findViewById<View>(R.id.survey_name)?.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun show_surveySelected_surveyIsActivated() {
        val listView = surveySelectorDialogFragment.dialog?.currentFocus as ListView?

        Mockito.`when`(mockLocalDataStore.getSurveyById(ArgumentMatchers.eq(survey2.id)))
            .thenReturn(Maybe.just(survey2))
        shadowOf(listView).performItemClick(1)
        shadowOf(Looper.getMainLooper()).idle()

        // Verify Dialog is dismissed
        assertThat(surveySelectorDialogFragment.dialog).isNull()
        surveyRepository.activeSurvey.test().assertValue(Optional.of(survey2))
    }
}