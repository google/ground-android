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
package org.groundplatform.android.ui.settings

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import java.util.Locale

fun onAttach(context: Context, defaultLanguage: String = "en"): Context {
  val lang = getPersistedLanguage(context, defaultLanguage)
  return setLocale(context, lang)
}

private fun getPersistedLanguage(context: Context, defaultLanguage: String): String {
  val preferences = PreferenceManager.getDefaultSharedPreferences(context)
  return preferences.getString("select_language", defaultLanguage) ?: defaultLanguage
}

fun setLocale(context: Context, language: String): Context {
  val locale = Locale(language)
  Locale.setDefault(locale)

  // Create a new configuration with the selected locale
  val config = Configuration(context.resources.configuration)
  config.setLocale(locale)

  // Return a context with the updated configuration.
  return context.createConfigurationContext(config)
}
