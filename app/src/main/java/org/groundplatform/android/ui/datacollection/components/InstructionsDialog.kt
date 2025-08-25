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

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun InstructionsDialog(iconId: Int, stringId: Int) {
  val showDialog = remember { mutableStateOf(true) }
  if (showDialog.value) {
    AlertDialog(
      icon = {
        Icon(
          imageVector = ImageVector.vectorResource(id = iconId),
          contentDescription = "",
          modifier = Modifier.width(48.dp).height(48.dp),
        )
      },
      title = { Text(text = stringResource(stringId), fontSize = 18.sp) },
      onDismissRequest = {}, // Prevent dismissing the dialog by clicking outside
      confirmButton = {}, // Hide confirm button
      dismissButton = {
        OutlinedButton(onClick = { showDialog.value = false }) {
          Text(text = stringResource(R.string.close))
        }
      },
    )
  }
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
fun PreviewInstructionsDialog() {
  AppTheme {
    InstructionsDialog(
      iconId = R.drawable.touch_app_24,
      stringId = R.string.draw_area_task_instruction,
    )
  }
}
