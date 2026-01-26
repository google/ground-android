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
package org.groundplatform.android.ui.compose

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.R
import org.groundplatform.android.ui.main.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ComposeE2ETest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testNavigationToSyncStatus() {
        // 1. Open Drawer
        composeTestRule.onNodeWithTag("open_nav_drawer").performClick()

        // 2. Click "Sync Status"
        val syncStatusText = composeTestRule.activity.getString(R.string.sync_status)
        composeTestRule.onNodeWithText(syncStatusText).performClick()

        // 3. Verify Sync Status Screen is shown (Title in TopAppBar)
        val syncStatusTitle = composeTestRule.activity.getString(R.string.data_sync_status)
        composeTestRule.onNodeWithText(syncStatusTitle).assertIsDisplayed()
    }

    @Test
    fun testNavigationToOfflineAreas() {
        // 1. Open Drawer
        composeTestRule.onNodeWithTag("open_nav_drawer").performClick()

        // 2. Click "Offline Map Imagery"
        val offlineMapText = composeTestRule.activity.getString(R.string.offline_map_imagery)
        composeTestRule.onNodeWithText(offlineMapText).performClick()

        // 3. Verify Offline Areas Screen is shown (Title in TopAppBar)
        // Note: OfflineAreasFragment label is @string/offline_map_imagery
        val offlineMapTitle = composeTestRule.activity.getString(R.string.offline_map_imagery)
        composeTestRule.onNodeWithText(offlineMapTitle).assertIsDisplayed()
    }
}
