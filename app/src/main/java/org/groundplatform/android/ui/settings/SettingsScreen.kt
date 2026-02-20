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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val languages = stringArrayResource(R.array.language_entries)
  val languageCodes = stringArrayResource(R.array.language_entry_values)
  val measurementUnits = stringArrayResource(R.array.length_entries)
  val measurementUnitValues = stringArrayResource(R.array.length_entry_values)

  uiState?.let { settings ->
    SettingsScreen(
      settings = settings,
      languages = languages.toList(),
      languageCodes = languageCodes.toList(),
      measurementUnits = measurementUnits.toList(),
      measurementUnitValues = measurementUnitValues.toList(),
      onUploadMediaOverUnmeteredConnectionOnlyChange = {
        viewModel.updateUploadMediaOverUnmeteredConnectionOnly(it)
      },
      onLanguageChange = { viewModel.updateSelectedLanguage(it) },
      onMeasurementUnitsChange = { viewModel.updateMeasurementUnits(it) },
      onVisitWebsiteClick = {
        val websiteUrl = context.getString(R.string.ground_website)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        context.startActivity(intent)
      },
    )
  }
}

@Composable
fun SettingsScreen(
  settings: UserSettings,
  languages: List<String>,
  languageCodes: List<String>,
  measurementUnits: List<String>,
  measurementUnitValues: List<String>,
  onUploadMediaOverUnmeteredConnectionOnlyChange: (Boolean) -> Unit,
  onLanguageChange: (String) -> Unit,
  onMeasurementUnitsChange: (MeasurementUnits) -> Unit,
  onVisitWebsiteClick: () -> Unit,
) {
  Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
    // General Section
    SettingsCategoryHeader(stringResource(R.string.general_title))

    // Upload Media
    SettingsSwitchItem(
      title = stringResource(R.string.upload_media_title),
      summary = stringResource(R.string.over_wifi_summary),
      checked = settings.shouldUploadPhotosOnWifiOnly,
      onCheckedChange = onUploadMediaOverUnmeteredConnectionOnlyChange,
    )

    // Language
    val currentLanguageIndex = languageCodes.indexOf(settings.language).takeIf { it >= 0 } ?: 0
    SettingsDialogItem(
      title = stringResource(R.string.select_language_title),
      summary = languages.getOrElse(currentLanguageIndex) { "" },
      dialogTitle = stringResource(R.string.select_language_title),
      options = languages,
      selectedIndex = currentLanguageIndex,
      onOptionSelected = { index -> onLanguageChange(languageCodes[index]) },
    )

    // Measurement Units
    val currentUnitIndex =
      measurementUnitValues.indexOf(settings.measurementUnits.name).takeIf { it >= 0 } ?: 0
    SettingsDialogItem(
      title = stringResource(R.string.select_length_title),
      summary = measurementUnits.getOrElse(currentUnitIndex) { "" },
      dialogTitle = stringResource(R.string.select_length_title),
      options = measurementUnits,
      selectedIndex = currentUnitIndex,
      onOptionSelected = { index ->
        val selectedUnit = MeasurementUnits.valueOf(measurementUnitValues[index])
        onMeasurementUnitsChange(selectedUnit)
      },
    )

    HorizontalDivider()

    // Help Section
    SettingsCategoryHeader(stringResource(R.string.help_title))

    // Visit Website
    SettingsItem(
      title = stringResource(R.string.visit_website_title),
      summary = stringResource(R.string.ground_website),
      onClick = onVisitWebsiteClick,
    )
  }
}

@Composable
fun SettingsCategoryHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
  )
}

@Composable
fun SettingsItem(title: String, summary: String? = null, onClick: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    if (summary != null) {
      Text(
        text = summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun SettingsSwitchItem(
  title: String,
  summary: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      if (summary != null) {
        Text(
          text = summary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun SettingsDialogItem(
  title: String,
  summary: String,
  dialogTitle: String,
  options: List<String>,
  selectedIndex: Int,
  onOptionSelected: (Int) -> Unit,
) {
  var showDialog by remember { mutableStateOf(false) }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = { Text(dialogTitle) },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          options.forEachIndexed { index, option ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    onOptionSelected(index)
                    showDialog = false
                  }
                  .padding(vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = index == selectedIndex,
                onClick = null, // Handled by Row clickable
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = option)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
      },
    )
  }

  SettingsItem(title = title, summary = summary, onClick = { showDialog = true })
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
  AppTheme {
    SettingsScreen(
      settings =
        UserSettings(
          language = "en",
          measurementUnits = MeasurementUnits.METRIC,
          shouldUploadPhotosOnWifiOnly = true,
        ),
      languages = listOf("English", "French", "Spanish"),
      languageCodes = listOf("en", "fr", "es"),
      measurementUnits = listOf("Metric", "Imperial"),
      measurementUnitValues = listOf("METRIC", "IMPERIAL"),
      onUploadMediaOverUnmeteredConnectionOnlyChange = {},
      onLanguageChange = {},
      onMeasurementUnitsChange = {},
      onVisitWebsiteClick = {},
    )
  }
}
