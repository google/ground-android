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
package com.google.android.ground.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.ground.ExcludeFromJacocoGeneratedReport
import com.google.android.ground.R
import com.google.android.ground.ui.theme.AppTheme

@Composable
fun SignOutConfirmationDialog(signOutCallback: () -> Unit, dismissCallback: () -> Unit) {
  AlertDialog(
    onDismissRequest = { dismissCallback() },
    title = { Text(text = stringResource(R.string.sign_out_dialog_title)) },
    text = { Text(text = stringResource(R.string.sign_out_dialog_body)) },
    dismissButton = {
      TextButton(onClick = { dismissCallback() }) { Text(text = stringResource(R.string.cancel)) }
    },
    confirmButton = {
      TextButton(
        onClick = {
          signOutCallback()
          dismissCallback()
        }
      ) {
        Text(text = stringResource(R.string.sign_out))
      }
    },
  )
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
fun PreviewSignOutConfirmationDialog() {
  AppTheme { SignOutConfirmationDialog(signOutCallback = {}, dismissCallback = {}) }
}
