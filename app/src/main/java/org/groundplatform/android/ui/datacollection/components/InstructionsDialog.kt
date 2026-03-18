/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.ui.datacollection.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.ui.theme.AppTheme

@Composable
fun InstructionsDialog(data: InstructionData, onDismissed: () -> Unit) {
  AlertDialog(
    icon = {
      Icon(
        imageVector = ImageVector.vectorResource(id = data.iconId),
        contentDescription = null,
        modifier = Modifier.size(48.dp),
      )
    },
    title = {
      Text(text = stringResource(data.stringId), style = MaterialTheme.typography.titleLarge)
    },
    onDismissRequest = {}, // Prevent dismissing the dialog by clicking outside
    confirmButton = {}, // Hide confirm button
    dismissButton = {
      OutlinedButton(onClick = onDismissed) { Text(text = stringResource(R.string.close)) }
    },
  )
}

/**
 * Data class representing the visual elements of an instruction dialog.
 *
 * @property iconId The resource ID of the drawable icon to display.
 * @property stringId The resource ID of the string message to display.
 */
data class InstructionData(val iconId: Int, val stringId: Int)

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
private fun PreviewInstructionsDialog() {
  AppTheme {
    InstructionsDialog(
      data =
        InstructionData(
          iconId = R.drawable.touch_app_24,
          stringId = R.string.draw_area_task_instruction,
        )
    ) {}
  }
}
