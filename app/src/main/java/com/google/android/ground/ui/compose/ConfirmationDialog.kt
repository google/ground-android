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
package com.google.android.ground.ui.compose

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.ground.ExcludeFromJacocoGeneratedReport
import com.google.android.ground.R
import com.google.android.ground.ui.theme.AppTheme

@Composable
fun ConfirmationDialog(
  @StringRes title: Int,
  @StringRes description: Int,
  @StringRes dismissButton: Int = R.string.cancel,
  @StringRes confirmButton: Int,
  onConfirmClicked: () -> Unit,
) {
  val showRemoveWarningDialog = remember { mutableStateOf(true) }

  fun onConfirm() {
    showRemoveWarningDialog.value = false
    onConfirmClicked()
  }

  fun onDismiss() {
    showRemoveWarningDialog.value = false
  }

  if (showRemoveWarningDialog.value) {
    AlertDialog(
      onDismissRequest = { onDismiss() },
      title = { Text(text = stringResource(title)) },
      text = { Text(text = stringResource(description)) },
      dismissButton = {
        TextButton(onClick = { onDismiss() }) { Text(text = stringResource(dismissButton)) }
      },
      confirmButton = {
        TextButton(onClick = { onConfirm() }) { Text(text = stringResource(confirmButton)) }
      },
    )
  }
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
fun PreviewConfirmationDialog() {
  AppTheme {
    ConfirmationDialog(
      title = R.string.data_collection_cancellation_title,
      description = R.string.data_collection_cancellation_description,
      confirmButton = R.string.data_collection_cancellation_confirm_button,
      onConfirmClicked = {},
    )
  }
}
