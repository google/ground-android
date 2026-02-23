package org.groundplatform.android.ui.settings.components

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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R

internal data class Option(val label: String, val value: String)

@Composable
internal fun SingleSelectionDialog(
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
