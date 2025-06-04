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
package org.groundplatform.android.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
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
    resetPreferences()
    launchFragmentInHiltContainer<SettingsFragment>() { fragment = this as SettingsFragment }
  }

  private fun resetPreferences() {
    PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
      .edit()
      .clear()
      .commit()

    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  @Test
  fun `General category items are Displayed`() {
    val item = fragment.findPreference<PreferenceCategory>("general_category")
    assertThat(item).isNotNull()
    assertThat(item!!.toString()).isEqualTo(fragment.getString(R.string.general_title))

    val items = item.getPreferenceCount()
    assertThat(items).isEqualTo(2)

    val preferenceGeneral = item.getPreference(0)
    assertThat(preferenceGeneral.title).isEqualTo(fragment.getString(R.string.upload_media_title))
    assertThat(preferenceGeneral.summary.toString())
      .isEqualTo(fragment.getString(R.string.over_wifi_summary))

    val preferenceLanguage = item.getPreference(1)
    assertThat(preferenceLanguage.title)
      .isEqualTo(fragment.getString(R.string.select_language_title))
    assertThat(preferenceLanguage.summary.toString()).isEqualTo("English") // default language
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

  @Test
  fun `When sharedPreferences is null, should use device default language`() =
    runWithTestDispatcher {
      val mockedPreferenceManager = mock<PreferenceManager>()
      whenever(mockedPreferenceManager.sharedPreferences).thenReturn(null)

      val generalCategory = fragment.findPreference<PreferenceCategory>("general_category")
      val languagePreference = generalCategory!!.getPreference(1) as? DropDownPreference
      val defaultLanguageCode = Locale.getDefault().language
      val expectedSummary =
        languagePreference?.let { pref ->
          val index = pref.findIndexOfValue(defaultLanguageCode)
          if (index >= 0) pref.entries[index].toString() else defaultLanguageCode
        } ?: defaultLanguageCode
      assertThat(languagePreference?.summary.toString()).isEqualTo(expectedSummary)
    }

  @Test
  fun `Change App Language to French`() {
    assertThat(fragment).isNotNull()

    val generalCategory = fragment.findPreference<PreferenceCategory>("general_category")
    assertThat(generalCategory).isNotNull()

    val languagePreference = generalCategory!!.getPreference(1) as? DropDownPreference
    assertThat(languagePreference).isNotNull()

    assertThat(languagePreference!!.summary.toString()).isEqualTo("English")

    val changeListener = languagePreference.onPreferenceChangeListener
    assertThat(changeListener).isNotNull()
  }
}
