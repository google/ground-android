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
package com.google.android.ground.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.ground.Config
import com.google.android.ground.R

/**
 * Fragment containing app preferences saved as shared preferences.
 *
 * NOTE: It uses [PreferenceFragmentCompat] instead of [ ], so dagger can't inject into it like it
 * does in other fragments.
 *
 * TODO: Create dagger module and support injection into this fragment.
 */
class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

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
}
