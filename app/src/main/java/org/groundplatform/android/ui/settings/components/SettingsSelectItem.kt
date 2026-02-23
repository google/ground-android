package org.groundplatform.android.ui.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources

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
  var showUnitDialog by remember { mutableStateOf(false) }

  if (showUnitDialog) {
    SingleSelectionDialog(
      title = title,
      options = allOptions,
      selectedOption = selectedOption,
      onOptionSelected = {
        onValueChanged(it.value)
        showUnitDialog = false
      },
      onDismiss = { showUnitDialog = false },
    )
  }

  SettingsItem(
    title = title,
    summary = selectedOption?.label ?: "",
    onClick = { showUnitDialog = true },
  )
}
