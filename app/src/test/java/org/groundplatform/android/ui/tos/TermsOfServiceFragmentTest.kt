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
package org.groundplatform.android.ui.tos

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.os.bundleOf
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.testrules.FragmentScenarioRule
import org.groundplatform.domain.model.TermsOfService
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TermsOfServiceFragmentTest : BaseHiltTest() {

  @get:Rule val fragmentScenario = FragmentScenarioRule()
  @get:Rule val composeTestRule = createComposeRule()

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  override fun setUp() {
    super.setUp()
    fakeRemoteDataStore.termsOfService = Result.success(TEST_TOS)
  }

  @Test
  fun `Terms of service text should be displayed`() {
    fragmentScenario.launchFragmentInHiltContainer<TermsOfServiceFragment>(
      bundleOf(Pair("isViewOnly", false))
    )

    composeTestRule.onNodeWithText("This is a heading\n\nSample terms of service\n\n").isDisplayed()

    composeTestRule
      .onNodeWithText(
        "<p dir=\"ltr\"><span style=\"font-size:1.50em;\"><b>This is a heading</b></span></p>\n" +
          "<p dir=\"ltr\">Sample terms of service</p>\n"
      )
      .isDisplayed()
  }

  companion object {
    const val TEST_TOS_TEXT = "# This is a heading\n\nSample terms of service"
    val TEST_TOS = TermsOfService("TERMS_OF_SERVICE", TEST_TOS_TEXT)
  }
}
