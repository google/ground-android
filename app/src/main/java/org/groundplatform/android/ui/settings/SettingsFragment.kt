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
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.Config
import org.groundplatform.android.MainActivity
import org.groundplatform.android.R
import org.groundplatform.android.persistence.local.LocalValueStore

/**
 * Fragment containing app preferences saved as shared preferences.
 *
 * NOTE: It uses [PreferenceFragmentCompat] instead of [ ], so dagger can't inject into it like it
 * does in other fragments.
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

  @Inject lateinit var localValueStore: LocalValueStore

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.sharedPreferencesName = Config.SHARED_PREFS_NAME
    preferenceManager.sharedPreferencesMode = Config.SHARED_PREFS_MODE

    setPreferencesFromResource(R.xml.preferences, rootKey)
    for (key in Keys.ALL_KEYS) {
      val preference =
        findPreference<Preference>(key)
          ?: throw IllegalArgumentException("Key not found in preferences.xml: $key")

      preference.onPreferenceClickListener = this
    }

    val switchPreference = findPreference<SwitchPreferenceCompat>(Keys.UPLOAD_MEDIA)
    switchPreference?.isChecked = loadSwitchPreferenceState()

    val languagePreference = findPreference<DropDownPreference>(Keys.LANGUAGE)
    val selectedLanguage = localValueStore.selectedLanguage
    languagePreference?.apply {
      val index = findIndexOfValue(selectedLanguage)
      if (index >= 0) {
        summary = entries[index]
      }

      onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
          if (newValue is String) {
            val index = findIndexOfValue(newValue)
            if (index >= 0) {
              summary = entries[index]
            }
            updateLocaleAndRestart(newValue)
          }
          true
        }
    }
  }

  private fun loadSwitchPreferenceState() =
    preferenceManager.sharedPreferences?.getBoolean(Keys.UPLOAD_MEDIA, false) ?: false

  override fun onPreferenceClick(preference: Preference): Boolean {
    when (preference.key) {
      Keys.VISIT_WEBSITE -> openUrl(preference.summary.toString())
      Keys.FEEDBACK ->
        Toast.makeText(context, R.string.not_yet_impl_title, Toast.LENGTH_SHORT).show()
    }
    return true
  }

  private fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
  }

  private fun updateLocaleAndRestart(languageCode: String) {
    preferenceManager.sharedPreferences?.edit()?.putString(Keys.LANGUAGE, languageCode)?.apply()

    val appLocale = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)

    val intent =
      Intent(requireContext(), MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    startActivity(intent)
  }
}
