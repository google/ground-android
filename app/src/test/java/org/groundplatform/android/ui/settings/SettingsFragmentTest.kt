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
import androidx.preference.get
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

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
  fun `category titles are displayed correctly`() {
    assertHasCategory("general_category").apply { assertThat(title).isEqualTo("General") }

    assertHasCategory("help_category").apply { assertThat(title).isEqualTo("Help") }
  }

  @Test
  fun `general category items are displayed`() {
    val category = assertHasCategory("general_category")

    assertThat(category.preferenceCount).isEqualTo(3)

    category.getPreference(0).apply {
      assertThat(title).isEqualTo("Upload photos")
      assertThat(summary).isEqualTo("Over Wi-Fi only")
    }

    category.getPreference(1).apply {
      assertThat(title).isEqualTo("Select language")
      assertThat(summary).isEqualTo("English")
    }

    category.getPreference(2).apply {
      assertThat(title).isEqualTo("Unit of length")
      assertThat(summary).isEqualTo("Meters (m)")
    }
  }

  @Test
  fun `help category items are displayed`() {
    val item = assertHasCategory("help_category")

    val items = item.preferenceCount
    assertThat(items).isEqualTo(1)

    val preferenceHelp = item.getPreference(0)
    assertThat(preferenceHelp.title.toString()).isEqualTo("Visit website")
    assertThat(preferenceHelp.summary).isEqualTo("https://groundplatform.org/")
  }

  @Test
  fun `visit website click opens correct page`() {
    val item = assertHasCategory("help_category")

    val preferenceWebsite = item.getPreference(0)
    preferenceWebsite.performClick()

    assertEquals(
      preferenceWebsite.summary,
      Shadows.shadowOf(fragment.activity).nextStartedActivity.data.toString(),
    )
  }

  @Test
  fun `when shared preferences is null should use device default language`() =
    runWithTestDispatcher {
      val mockedPreferenceManager = mock<PreferenceManager>()
      whenever(mockedPreferenceManager.sharedPreferences).thenReturn(null)

      val generalCategory = assertHasCategory("general_category")
      val languagePreference = generalCategory.getPreference(1) as? DropDownPreference
      val defaultLanguageCode = Locale.getDefault().language
      val expectedSummary =
        languagePreference?.let { pref ->
          val index = pref.findIndexOfValue(defaultLanguageCode)
          if (index >= 0) pref.entries[index].toString() else defaultLanguageCode
        } ?: defaultLanguageCode
      assertThat(languagePreference?.summary).isEqualTo(expectedSummary)
    }

  @Test
  fun `change app language to french`() {
    val generalCategory = assertHasCategory("general_category")

    val languagePreference = generalCategory.getPreference(1) as? DropDownPreference
    assertThat(languagePreference).isNotNull()
    assertThat(languagePreference!!.summary).isEqualTo("English")

    val changeListener = languagePreference.onPreferenceChangeListener
    assertThat(changeListener).isNotNull()
  }

  private fun assertHasCategory(key: String): PreferenceCategory {
    val item = fragment.findPreference<PreferenceCategory>(key)
    assertThat(item).isNotNull()
    return item!!
  }
}
