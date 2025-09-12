/*
 * Copyright 2020 Google LLC
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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.common.Constants
import org.groundplatform.android.common.PrefKeys
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.ui.main.MainActivity

/** Fragment containing app preferences saved as shared preferences. */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

  @Inject lateinit var localValueStore: LocalValueStore

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.sharedPreferencesName = Constants.SHARED_PREFS_NAME
    preferenceManager.sharedPreferencesMode = Constants.SHARED_PREFS_MODE

    setPreferencesFromResource(R.xml.preferences, rootKey)
    for (key in ALL_KEYS) {
      val preference =
        findPreference<Preference>(key)
          ?: throw IllegalArgumentException("Key not found in preferences.xml: $key")

      preference.onPreferenceClickListener = this
    }

    val switchPreference = findPreference<SwitchPreferenceCompat>(PrefKeys.UPLOAD_MEDIA)
    switchPreference?.isChecked = loadSwitchPreferenceState()

    setupDropDownPreference(PrefKeys.LANGUAGE, localValueStore.selectedLanguage) {
      updateLocaleAndRestart(it)
    }
    setupDropDownPreference(PrefKeys.LENGTH_UNIT, localValueStore.selectedLengthUnit)
  }

  private fun setupDropDownPreference(
    prefKey: String,
    selectedValue: String,
    onPrefChanged: (String) -> Unit = {},
  ) {
    findPreference<DropDownPreference>(prefKey)?.apply {
      updateSummary(selectedValue)
      onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
          if (newValue is String) {
            updateSummary(newValue)
            onPrefChanged(newValue)
          }
          true
        }
    }
  }

  private fun DropDownPreference.updateSummary(value: String) {
    val index = findIndexOfValue(value)
    if (index >= 0) {
      summary = entries[index]
    }
  }

  private fun loadSwitchPreferenceState() =
    preferenceManager.sharedPreferences?.getBoolean(PrefKeys.UPLOAD_MEDIA, false) ?: false

  override fun onPreferenceClick(preference: Preference): Boolean {
    if (preference.key == PrefKeys.VISIT_WEBSITE) {
      openUrl(preference.summary.toString())
    }
    return true
  }

  private fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    startActivity(intent)
  }

  private fun updateLocaleAndRestart(languageCode: String) {
    preferenceManager.sharedPreferences?.edit()?.putString(PrefKeys.LANGUAGE, languageCode)?.apply()

    val appLocale = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)

    val intent =
      Intent(requireContext(), MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    startActivity(intent)
  }

  companion object {
    private val ALL_KEYS =
      arrayOf(
        PrefKeys.LANGUAGE,
        PrefKeys.LENGTH_UNIT,
        PrefKeys.UPLOAD_MEDIA,
        PrefKeys.VISIT_WEBSITE,
      )
  }
}
