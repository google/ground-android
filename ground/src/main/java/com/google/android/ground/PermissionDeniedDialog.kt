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
package com.google.android.ground

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.ground.ui.compose.HyperlinkText
import com.google.android.ground.ui.theme.AppTheme
import timber.log.Timber

@Composable
fun PermissionDeniedDialog(signupLink: String?, onSignOut: () -> Unit, onCloseApp: () -> Unit) {
  var showDialog by remember { mutableStateOf(true) }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            painter = painterResource(id = R.drawable.baseline_warning_24),
            contentDescription = "Alert Icon",
            tint = MaterialTheme.colorScheme.error,
          )

          // Add some space between icon and title
          Spacer(modifier = Modifier.width(12.dp))

          Text(
            stringResource(id = R.string.permission_denied),
            style = MaterialTheme.typography.titleLarge,
          )
        }
      },
      text = {
        Column {
          if (signupLink.isNullOrEmpty()) {
            Text(
              stringResource(R.string.admin_request_access),
              style = MaterialTheme.typography.bodyMedium,
            )
          } else {
            val uriHandler = LocalUriHandler.current
            HyperlinkText(
              modifier = Modifier.fillMaxWidth(),
              textStyle = MaterialTheme.typography.bodyMedium,
              fullTextResId = R.string.signup_request_access,
              linkTextColor = MaterialTheme.colorScheme.primary,
              hyperLinks =
                mapOf(
                  "sign_up_link" to
                    {
                      runCatching { uriHandler.openUri(signupLink) }
                        .onFailure { Timber.e(it, "Failed to open signup link") }
                    }
                ),
            )
          }

          // Empty line
          Text(text = "")

          Text(
            stringResource(R.string.signout_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            showDialog = false
            onSignOut()
          }
        ) {
          Text(stringResource(id = R.string.sign_out))
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showDialog = false
            onCloseApp()
          }
        ) {
          Text(stringResource(id = R.string.close_app))
        }
      },
    )
  }
}

@Composable
@Preview
private fun PermissionDeniedDialogWithSignupLinkPreview() {
  AppTheme {
    PermissionDeniedDialog(signupLink = "www.google.com", onSignOut = {}, onCloseApp = {})
  }
}

@Composable
@Preview
private fun PermissionDeniedDialogWithoutSignupLinkPreview() {
  AppTheme { PermissionDeniedDialog(signupLink = null, onSignOut = {}, onCloseApp = {}) }
}
