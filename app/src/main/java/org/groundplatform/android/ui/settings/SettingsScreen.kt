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

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.android.ui.settings.components.Option
import org.groundplatform.android.ui.settings.components.SettingsCategory
import org.groundplatform.android.ui.settings.components.SettingsItem
import org.groundplatform.android.ui.settings.components.SettingsSwitchItem
import org.groundplatform.android.ui.settings.components.SingleSelectionDialog
import org.groundplatform.android.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  onLocaleChanged: (String) -> Unit,
  onVisitWebsiteClick: (url: Uri) -> Unit,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val websiteUrl = stringResource(R.string.ground_website)

  if (uiState == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
  } else {
    SettingsScreen(
      settings = uiState!!,
      onUploadMediaOverUnmeteredConnectionOnlyChange = {
        viewModel.updateUploadMediaOverUnmeteredConnectionOnly(it)
      },
      onLanguageChange = {
        viewModel.updateSelectedLanguage(it)
        onLocaleChanged(it)
      },
      onMeasurementUnitsChange = { viewModel.updateMeasurementUnits(it) },
      onVisitWebsiteClick = { onVisitWebsiteClick(websiteUrl.toUri()) },
      onBack = onBack,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting
@Composable
internal fun SettingsScreen(
  settings: UserSettings,
  onUploadMediaOverUnmeteredConnectionOnlyChange: (Boolean) -> Unit,
  onLanguageChange: (String) -> Unit,
  onMeasurementUnitsChange: (MeasurementUnits) -> Unit,
  onVisitWebsiteClick: () -> Unit,
  onBack: () -> Unit,
) {

  Scaffold(
    topBar = {
      Toolbar(stringRes = R.string.settings, showNavigationIcon = true, iconClick = onBack)
    }
  ) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
      // General Section
      SettingsCategory(stringResource(R.string.general_title)) {
        // Upload Media
        SettingsSwitchItem(
          title = stringResource(R.string.upload_media_title),
          summary = stringResource(R.string.over_wifi_summary),
          checked = settings.shouldUploadPhotosOnWifiOnly,
          onCheckedChange = onUploadMediaOverUnmeteredConnectionOnlyChange,
        )

        // Language
        SelectLanguageSetting(settings.language, onLanguageChange)

        // Measurement Units
        SelectUnitSetting(settings.measurementUnits.name, onMeasurementUnitsChange)
      }

      HorizontalDivider()

      // Help Section
      SettingsCategory(stringResource(R.string.help_title)) {
        SettingsItem(
          title = stringResource(R.string.visit_website_title),
          summary = stringResource(R.string.ground_website),
          onClick = onVisitWebsiteClick,
        )
      }
    }
  }
}

@Composable
private fun SelectLanguageSetting(currentValue: String, onLanguageChange: (String) -> Unit) {
  val configuration = LocalConfiguration.current
  val resources = LocalResources.current

  val languages =
    remember(configuration) {
      val labels = resources.getStringArray(R.array.language_entries)
      val values = resources.getStringArray(R.array.language_entry_values)
      labels.zip(values) { label, value -> Option(label, value) }
    }

  val currentLanguage = languages.find { it.value == currentValue } ?: languages.firstOrNull()
  var showLanguageDialog by remember { mutableStateOf(false) }

  if (showLanguageDialog) {
    SingleSelectionDialog(
      title = stringResource(R.string.select_language_title),
      options = languages,
      selectedOption = currentLanguage,
      onOptionSelected = {
        onLanguageChange(it.value)
        showLanguageDialog = false
      },
      onDismiss = { showLanguageDialog = false },
    )
  }

  SettingsItem(
    title = stringResource(R.string.select_language_title),
    summary = currentLanguage?.label ?: "",
    onClick = { showLanguageDialog = true },
  )
}

@Composable
private fun SelectUnitSetting(
  currentValue: String,
  onMeasurementUnitsChange: (MeasurementUnits) -> Unit,
) {
  val configuration = LocalConfiguration.current
  val resources = LocalResources.current

  val measurementUnits =
    remember(configuration) {
      val labels = resources.getStringArray(R.array.length_entries)
      val values = resources.getStringArray(R.array.length_entry_values)
      labels.zip(values) { label, value -> Option(label, value) }
    }

  val currentUnit =
    measurementUnits.find { it.value == currentValue } ?: measurementUnits.firstOrNull()
  var showUnitDialog by remember { mutableStateOf(false) }

  if (showUnitDialog) {
    SingleSelectionDialog(
      title = stringResource(R.string.select_length_title),
      options = measurementUnits,
      selectedOption = currentUnit,
      onOptionSelected = {
        onMeasurementUnitsChange(MeasurementUnits.valueOf(it.value))
        showUnitDialog = false
      },
      onDismiss = { showUnitDialog = false },
    )
  }

  SettingsItem(
    title = stringResource(R.string.select_length_title),
    summary = currentUnit?.label ?: "",
    onClick = { showUnitDialog = true },
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SettingsScreenPreview() {
  AppTheme {
    SettingsScreen(
      settings =
        UserSettings(
          language = "en",
          measurementUnits = MeasurementUnits.METRIC,
          shouldUploadPhotosOnWifiOnly = true,
        ),
      onUploadMediaOverUnmeteredConnectionOnlyChange = {},
      onLanguageChange = {},
      onMeasurementUnitsChange = {},
      onVisitWebsiteClick = {},
      onBack = {},
    )
  }
}
