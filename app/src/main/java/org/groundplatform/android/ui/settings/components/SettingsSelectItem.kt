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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

/**
 * A settings item that allows users to select a single value from a list of options.
 *
 * When clicked, it displays a dropdown menu with options populated from the provided resource IDs.
 *
 * @param title The title of the settings item.
 * @param entriesResId The resource ID of the string array containing the display labels.
 * @param entryValues The resource ID of the string array containing the underlying values.
 * @param currentValue The currently selected value.
 * @param onValueChanged Callback triggered when a new value is selected.
 */
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
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxWidth()) {
    SettingsItem(
      title = title,
      summary = selectedOption?.label ?: "",
      onClick = { expanded = true },
    )

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      offset = DpOffset(16.dp, 0.dp),
      modifier = Modifier.widthIn(min = 200.dp),
    ) {
      allOptions.forEach { option ->
        DropdownMenuItem(
          text = { Text(text = option.label) },
          onClick = {
            onValueChanged(option.value)
            expanded = false
          },
        )
      }
    }
  }
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
