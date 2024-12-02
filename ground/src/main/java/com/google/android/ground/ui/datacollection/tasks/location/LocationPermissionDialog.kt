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
package com.google.android.ground.ui.datacollection.tasks.location

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.ground.ExcludeFromJacocoGeneratedReport
import com.google.android.ground.R
import com.google.android.ground.ui.theme.AppTheme

@Composable
fun LocationPermissionDialog(onDismiss: () -> Unit, onCancel: () -> Unit) {
  val context = LocalContext.current
  AlertDialog(
    containerColor = MaterialTheme.colorScheme.surface,
    onDismissRequest = { onDismiss() },
    title = {
      Text(
        stringResource(R.string.allow_location_title),
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Text(
        stringResource(R.string.allow_location_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    dismissButton = {
      TextButton(onClick = { onCancel() }) { Text(text = stringResource(R.string.cancel)) }
    },
    confirmButton = {
      TextButton(
        onClick = {
          // Open the app settings
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          val uri = Uri.fromParts("package", context.packageName, null)
          intent.data = uri
          context.startActivity(intent)
        }
      ) {
        Text(text = stringResource(R.string.allow_location_confirmation))
      }
    },
  )
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
fun PreviewUserDetailsDialog() {
  AppTheme { LocationPermissionDialog({}, {}) }
}
