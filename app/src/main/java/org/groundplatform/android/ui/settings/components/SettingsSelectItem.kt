/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
internal fun SettingsSelectItem(
  title: String,
  entriesResId: Int,
  entryValues: Int,
  currentValue: String,
  onValueChanged: (String) -> Unit,
) {
  val configuration = LocalConfiguration.current
  val resources = LocalResources.current

  val allOptions =
    remember(configuration) {
      val labels = resources.getStringArray(entriesResId)
      val values = resources.getStringArray(entryValues)
      labels.zip(values) { label, value -> Option(label, value) }
    }

  val selectedOption = allOptions.find { it.value == currentValue } ?: allOptions.firstOrNull()
  var showDialog by remember { mutableStateOf(false) }

  if (showDialog) {
    SingleSelectionDialog(
      title = title,
      options = allOptions,
      selectedOption = selectedOption,
      onOptionSelected = {
        onValueChanged(it)
        showDialog = false
      },
      onDismiss = { showDialog = false },
    )
  }

  SettingsItem(
    title = title,
    summary = selectedOption?.label ?: "",
    onClick = { showDialog = true },
  )
}

internal data class Option(val label: String, val value: String)

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun PreviewSelectItem() {
  AppTheme {
    SettingsSelectItem(
      title = "Language",
      entriesResId = R.array.language_entries,
      entryValues = R.array.language_entry_values,
      currentValue = "en",
      onValueChanged = {},
    )
  }
}
