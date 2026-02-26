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
package org.groundplatform.android.ui.settings

import android.content.pm.PackageInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.groundplatform.android.R
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

  @Before
  fun setUp() {
    val packageInfo = PackageInfo()
    packageInfo.packageName = "org.groundplatform.android"
    Shadows.shadowOf(context.packageManager).installPackage(packageInfo)
  }

  @Test
  fun testSettingsScreen_InitialState() {
    val settings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.METRIC,
        shouldUploadPhotosOnWifiOnly = true,
      )

    composeTestRule.setContent {
      AppTheme {
        SettingsScreen(
          settings = settings,
          onUploadMediaOverUnmeteredConnectionOnlyChange = {},
          onLanguageChange = {},
          onMeasurementUnitsChange = {},
          onVisitWebsiteClick = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithText(context.getString(R.string.general_title)).assertIsDisplayed()
    composeTestRule
      .onNodeWithText(context.getString(R.string.upload_media_title))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(context.getString(R.string.select_language_title))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(context.getString(R.string.select_length_title))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(context.getString(R.string.help_title))
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(context.getString(R.string.visit_website_title))
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun testSettingsScreen_ToggleUploadMedia() {
    var uploadMediaChecked = false
    val settings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.METRIC,
        shouldUploadPhotosOnWifiOnly = false,
      )

    composeTestRule.setContent {
      AppTheme {
        SettingsScreen(
          settings = settings,
          onUploadMediaOverUnmeteredConnectionOnlyChange = { uploadMediaChecked = it },
          onLanguageChange = {},
          onMeasurementUnitsChange = {},
          onVisitWebsiteClick = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithText(context.getString(R.string.upload_media_title)).performClick()
    assert(uploadMediaChecked)
  }

  @Test
  fun testSettingsScreen_ChangeLanguage() {
    var selectedLanguage: String? = null
    val settings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.METRIC,
        shouldUploadPhotosOnWifiOnly = false,
      )

    composeTestRule.setContent {
      AppTheme {
        SettingsScreen(
          settings = settings,
          onUploadMediaOverUnmeteredConnectionOnlyChange = {},
          onLanguageChange = { selectedLanguage = it },
          onMeasurementUnitsChange = {},
          onVisitWebsiteClick = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithText(context.getString(R.string.select_language_title)).performClick()
    composeTestRule.onNodeWithText(context.getString(R.string.lang_french)).performClick()
    assert(selectedLanguage == "fr")
  }

  @Test
  fun testSettingsScreen_ChangeUnits() {
    var selectedUnits: MeasurementUnits? = null
    val settings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.METRIC,
        shouldUploadPhotosOnWifiOnly = false,
      )

    composeTestRule.setContent {
      AppTheme {
        SettingsScreen(
          settings = settings,
          onUploadMediaOverUnmeteredConnectionOnlyChange = {},
          onLanguageChange = {},
          onMeasurementUnitsChange = { selectedUnits = it },
          onVisitWebsiteClick = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithText(context.getString(R.string.select_length_title)).performClick()
    composeTestRule.onNodeWithText(context.getString(R.string.length_imperial)).performClick()
    assert(selectedUnits == MeasurementUnits.IMPERIAL)
  }

  @Test
  fun testSettingsScreen_VisitWebsite() {
    var visited = false
    val settings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.METRIC,
        shouldUploadPhotosOnWifiOnly = false,
      )

    composeTestRule.setContent {
      AppTheme {
        SettingsScreen(
          settings = settings,
          onUploadMediaOverUnmeteredConnectionOnlyChange = {},
          onLanguageChange = {},
          onMeasurementUnitsChange = {},
          onVisitWebsiteClick = { visited = true },
          onBack = {},
        )
      }
    }

    composeTestRule
      .onNodeWithText(context.getString(R.string.visit_website_title))
      .performScrollTo()
      .performClick()
    assert(visited)
  }
}
