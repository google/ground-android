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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.android.ui.theme.AppTheme
import androidx.compose.ui.platform.LocalResources

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

private data class Option(val label: String, val value: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
  settings: UserSettings,
  onUploadMediaOverUnmeteredConnectionOnlyChange: (Boolean) -> Unit,
  onLanguageChange: (String) -> Unit,
  onMeasurementUnitsChange: (MeasurementUnits) -> Unit,
  onVisitWebsiteClick: () -> Unit,
  onBack: () -> Unit,
) {
  val configuration = LocalConfiguration.current
  val resources = LocalResources.current

  val languages =
    remember(configuration) {
      val labels = resources.getStringArray(R.array.language_entries)
      val values = resources.getStringArray(R.array.language_entry_values)
      labels.zip(values) { label, value -> Option(label, value) }
    }

  val measurementUnits =
    remember(configuration) {
      val labels = resources.getStringArray(R.array.length_entries)
      val values = resources.getStringArray(R.array.length_entry_values)
      labels.zip(values) { label, value -> Option(label, value) }
    }

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
          icon = Icons.Default.Share,
          checked = settings.shouldUploadPhotosOnWifiOnly,
          onCheckedChange = onUploadMediaOverUnmeteredConnectionOnlyChange,
        )

        // Language
        val currentLanguage =
          languages.find { it.value == settings.language } ?: languages.firstOrNull()
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
          icon = Icons.Default.Person,
          onClick = { showLanguageDialog = true },
        )

        // Measurement Units
        val currentUnit =
          measurementUnits.find { it.value == settings.measurementUnits.name }
            ?: measurementUnits.firstOrNull()
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
          icon = Icons.Default.Edit,
          onClick = { showUnitDialog = true },
        )
      }

      HorizontalDivider()

      // Help Section
      SettingsCategory(stringResource(R.string.help_title)) {
        SettingsItem(
          title = stringResource(R.string.visit_website_title),
          summary = stringResource(R.string.ground_website),
          icon = Icons.Default.Info,
          onClick = onVisitWebsiteClick,
        )
      }
    }
  }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
    content()
  }
}

@Composable
private fun SettingsItem(
  title: String,
  summary: String? = null,
  icon: ImageVector? = null,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth().clickable(onClick = onClick, role = Role.Button).padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.padding(end = 16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
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
  }
}

@Composable
private fun SettingsSwitchItem(
  title: String,
  summary: String? = null,
  icon: ImageVector? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.padding(end = 16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
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
    Switch(checked = checked, onCheckedChange = null)
  }
}

@Composable
private fun SingleSelectionDialog(
  title: String,
  options: List<Option>,
  selectedOption: Option?,
  onOptionSelected: (Option) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        options.forEach { option ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onOptionSelected(option) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = option == selectedOption, onClick = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = option.label)
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
