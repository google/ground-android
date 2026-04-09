/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.datacollection

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.android.ui.components.TEST_TAG_SHARE_LOI_COMPONENT
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataSubmissionConfirmationScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Shows the correct content on portrait`() {
    composeTestRule.setContent {
      DataSubmissionConfirmationScreen(loiReport = LOI_REPORT, onDismissed = {})
    }

    composeTestRule
      .onNodeWithText(getString(R.string.data_collection_complete_details))
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.share_location)).assertIsDisplayed()
    composeTestRule.onNodeWithText(LOI_REPORT.loiName).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SHARE_LOI_COMPONENT).assertIsDisplayed()
    composeTestRule
      .onNodeWithText(getString(R.string.scan_this_qr_to_download_geojson))
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.close)).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `Shows the correct content on landscape`() {
    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalConfiguration provides
          Configuration().apply { orientation = Configuration.ORIENTATION_LANDSCAPE }
      ) {
        DataSubmissionConfirmationScreen(loiReport = LOI_REPORT, onDismissed = {})
      }
    }

    composeTestRule
      .onNodeWithText(getString(R.string.data_collection_complete_details))
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.share_location)).assertIsDisplayed()
    composeTestRule.onNodeWithText(LOI_REPORT.loiName).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SHARE_LOI_COMPONENT).assertIsDisplayed()
    composeTestRule
      .onNodeWithText(getString(R.string.scan_this_qr_to_download_geojson))
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.close)).assertIsDisplayed()
  }

  @Test
  fun `Does not show QR section if the LoiReport is null`() {
    composeTestRule.setContent {
      DataSubmissionConfirmationScreen(loiReport = null, onDismissed = {})
    }

    composeTestRule
      .onNodeWithText(getString(R.string.data_collection_complete_details))
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.share_location)).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TEST_TAG_SHARE_LOI_COMPONENT).assertDoesNotExist()
    composeTestRule.onNodeWithText(getString(R.string.close)).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `onDismiss is triggered when the close button is clicked`() {
    var dismissed = false

    composeTestRule.setContent {
      DataSubmissionConfirmationScreen(loiReport = LOI_REPORT, onDismissed = { dismissed = true })
    }

    composeTestRule.onNodeWithText(getString(R.string.close)).performScrollTo().performClick()

    composeTestRule.runOnIdle { assertTrue(dismissed) }
  }

  private companion object {
    private val LOI_REPORT =
      LoiReport(
        loiName = "Test LOI",
        geoJson =
          JsonObject(
            mapOf(
              "type" to JsonPrimitive("Feature"),
              "properties" to JsonObject(mapOf("name" to JsonPrimitive("Test LOI"))),
              "geometry" to
                JsonObject(
                  mapOf(
                    "type" to JsonPrimitive("Point"),
                    "coordinates" to JsonArray(listOf(JsonPrimitive(20.0), JsonPrimitive(20.0))),
                  )
                ),
            )
          ),
      )
  }
}
