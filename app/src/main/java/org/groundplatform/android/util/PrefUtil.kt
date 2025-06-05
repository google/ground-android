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
package org.groundplatform.android.util

import android.content.Context
import androidx.preference.PreferenceManager
import java.util.Locale
import org.groundplatform.android.ui.settings.Keys

/**
 * Returns the selected app language from shared preferences.
 *
 * This function checks the user's language preference stored in shared preferences. If no
 * preference is set, it falls back to the device's default language.
 *
 * @param context The context used to access shared preferences.
 * @return The selected language code (e.g., "en", "fr").
 */
fun getSelectedLanguage(context: Context): String {
  val prefs = PreferenceManager.getDefaultSharedPreferences(context)
  val defaultLanguage = Locale.getDefault().language
  return prefs.getString(Keys.LANGUAGE, defaultLanguage) ?: defaultLanguage
}
