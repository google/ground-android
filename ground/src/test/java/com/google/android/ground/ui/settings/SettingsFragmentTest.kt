/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.ui.settings

import androidx.preference.PreferenceCategory
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SettingsFragmentTest : BaseHiltTest() {

  private lateinit var fragment: SettingsFragment

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentInHiltContainer<SettingsFragment> { fragment = this as SettingsFragment }
  }

  @Test
  fun `General category items are Displayed`() {
    val item = fragment.findPreference<PreferenceCategory>("general_category")
    assertThat(item).isNotNull()
    assertThat(item!!.toString()).isEqualTo(fragment.getString(R.string.general_title))

    val items = item.getPreferenceCount()
    assertThat(items).isEqualTo(1)

    val preferenceGeneral = item.getPreference(0)
    assertThat(preferenceGeneral.title).isEqualTo(fragment.getString(R.string.upload_media_title))
    assertThat(preferenceGeneral.summary.toString())
      .isEqualTo(fragment.getString(R.string.over_wifi_summary))
  }

  @Test
  fun `Help category items are Displayed`() {
    val item = fragment.findPreference<PreferenceCategory>("help_category")
    assertThat(item).isNotNull()

    val items = item!!.getPreferenceCount()
    assertThat(items).isEqualTo(2)

    val preferenceHelp = item.getPreference(0)
    assertThat(preferenceHelp.title.toString())
      .isEqualTo(fragment.getString(R.string.visit_website_title))
    assertThat(preferenceHelp.summary.toString())
      .isEqualTo(fragment.getString(R.string.ground_website))

    val preferenceFeedback = item.getPreference(1)
    assertThat(preferenceFeedback.title.toString())
      .isEqualTo(fragment.getString(R.string.send_feedback_title))
    assertThat(preferenceFeedback.summary.toString())
      .isEqualTo(fragment.getString(R.string.report_summary))
  }

  @Test
  fun `Feedback Toast is Displayed`() {
    val item = fragment.findPreference<PreferenceCategory>("help_category")

    val preferenceFeedback = item!!.getPreference(1)
    preferenceFeedback.performClick()
    assertThat(ShadowToast.getTextOfLatestToast())
      .isEqualTo(fragment.getString(R.string.not_yet_impl_title))
  }

  @Test
  fun `Visit Website click opens correct page`() {
    val item = fragment.findPreference<PreferenceCategory>("help_category")

    val preferenceWebsite = item!!.getPreference(0)
    preferenceWebsite.performClick()

    assertEquals(
      preferenceWebsite.summary,
      Shadows.shadowOf(fragment.activity).nextStartedActivity.data.toString(),
    )
  }
}
