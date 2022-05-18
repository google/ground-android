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

package com.google.android.gnd

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gnd.repository.MapsRepository
import com.google.android.gnd.system.auth.FakeAuthenticationManager
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class MapTypeDialogTest : BaseMainActivityTest() {

    @Inject
    lateinit var fakeAuthenticationManager: FakeAuthenticationManager

    @Inject
    lateinit var mapsRepository: MapsRepository

    override fun setUp() {
        super.setUp()
        fakeAuthenticationManager.setUser(FakeData.USER)
    }

    @Test
    fun tappingMapTypeButton_shouldOpenDialog() {
        dataBindingIdlingResource.monitorActivity(scenarioRule.scenario)
        skipTermsOfServiceFragment()

        onView(withId(R.id.map_type_btn)).perform(click())

        onView(withText("Map Type")).check(matches(isDisplayed()))
        onView(withText("Road Map")).check(matches(isDisplayed()))
        onView(withText("Terrain")).check(matches(isDisplayed()))
        onView(withText("Satellite")).check(matches(isDisplayed()))
    }

    @Test
    fun selectingMapTypeItem_shouldUpdateBasemapType() {
        dataBindingIdlingResource.monitorActivity(scenarioRule.scenario)
        skipTermsOfServiceFragment()

        Truth.assertThat(mapsRepository.mapType).isEqualTo(GoogleMap.MAP_TYPE_HYBRID)

        onView(withId(R.id.map_type_btn)).perform(click())
        onView(withText("Terrain")).perform(click())

        Truth.assertThat(mapsRepository.mapType).isEqualTo(GoogleMap.MAP_TYPE_TERRAIN)
    }
}
