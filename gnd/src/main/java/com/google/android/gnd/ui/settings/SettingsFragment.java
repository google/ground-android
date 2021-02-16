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

package com.google.android.gnd.ui.settings;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.google.android.gnd.Config;
import com.google.android.gnd.R;

/**
 * Fragment containing app preferences saved as shared preferences.
 *
 * <p>NOTE: It uses {@link PreferenceFragmentCompat} instead of {@link
 * com.google.android.gnd.ui.common.AbstractFragment}, so dagger can't inject into it like it does
 * in other fragments.
 *
 * <p>TODO: Create dagger module and support injection into this fragment.
 */
public class SettingsFragment extends PreferenceFragmentCompat
    implements OnPreferenceChangeListener, OnPreferenceClickListener {

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.setSharedPreferencesName(Config.SHARED_PREFS_NAME);
    preferenceManager.setSharedPreferencesMode(Config.SHARED_PREFS_MODE);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.preferences, rootKey);
    for (String key : Keys.ALL_KEYS) {
      Preference preference = findPreference(key);
      if (preference == null) {
        throw new IllegalArgumentException("Key not found in preferences.xml: " + key);
      }

      preference.setOnPreferenceChangeListener(this);
      preference.setOnPreferenceClickListener(this);
    }
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    switch (preference.getKey()) {
      case Keys.UPLOAD_MEDIA:
      case Keys.OFFLINE_AREAS:
        // do nothing.
        break;
      default:
        break;
    }
    return true;
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    switch (preference.getKey()) {
      case Keys.VISIT_WEBSITE:
        openUrl(getString(R.string.ground_website));
        break;
      case Keys.FEEDBACK:
        openUrl(getString(R.string.ground_android_github));
        break;
      default:
        return false;
    }
    return true;
  }

  private void openUrl(String url) {
    CustomTabColorSchemeParams params =
        new CustomTabColorSchemeParams.Builder()
            .setToolbarColor(getResources().getColor(R.color.colorPrimary))
            .build();

    CustomTabsIntent customTabsIntent =
        new CustomTabsIntent.Builder().setDefaultColorSchemeParams(params).build();

    customTabsIntent.launchUrl(requireContext(), Uri.parse(url));
  }
}
