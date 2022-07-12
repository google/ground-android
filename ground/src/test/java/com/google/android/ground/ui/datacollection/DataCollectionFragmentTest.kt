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
package com.google.android.ground.ui.datacollection

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.MainActivity
import com.google.android.ground.ui.common.Navigator
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionFragmentTest : BaseHiltTest() {

    private lateinit var dataCollectionFragment: DataCollectionFragment

    private val surveyId: String = "123"
    private val loiId: String = "456"
    private val submissionId: String = "789"

    // TODO(jsunde): populate some data for the job and LOI

    @BindValue
    @Mock
    lateinit var dataCollectionViewModel: DataCollectionViewModel

    @Inject
    lateinit var navigator: Navigator

    @Before
    override fun setUp() {
        super.setUp()

        val activityController = Robolectric.buildActivity(
            MainActivity::class.java
        ).setup()

        val activity = activityController.get()

        // TODO(jsunde): Figure out the best way to instantiate a DataCollectionFragment here with
        //  with the DataCollectionFragmentArgs supplied. The navigator.navigate() call doesn't
        //  seem to work because the HomeScreenFragment isn't currently open.
        //        navigator.navigate(
        //            HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
        //                surveyId, loiId,
        //                submissionId
        //            )
        //        )
        activity.supportFragmentManager.beginTransaction().add(dataCollectionFragment, null)
    }

    @Test
    fun created_submissionIsLoaded() {
        verify(dataCollectionViewModel).loadSubmissionDetails(
            DataCollectionFragmentArgs.Builder(
                surveyId,
                loiId,
                submissionId
            ).build()
        )
    }

}