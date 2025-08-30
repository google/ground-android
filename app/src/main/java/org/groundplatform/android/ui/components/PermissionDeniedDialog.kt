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
package org.groundplatform.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme
import timber.log.Timber

@Composable
fun PermissionDeniedDialog(signupLink: String, onSignOut: () -> Unit, onCloseApp: () -> Unit) {
  AlertDialog(
    onDismissRequest = {},
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
        if (signupLink.isNotEmpty()) {
          SignupLink(signupLink)
        } else {
          Text(
            stringResource(R.string.admin_request_access),
            style = MaterialTheme.typography.bodyMedium,
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
      OutlinedButton(onClick = { onSignOut() }) { Text(stringResource(id = R.string.sign_out)) }
    },
    confirmButton = {
      Button(onClick = { onCloseApp() }) { Text(stringResource(id = R.string.close_app)) }
    },
  )
}

@Composable
private fun SignupLink(signupLink: String) {
  val uriHandler = LocalUriHandler.current

  HyperlinkText(
    textStyle = MaterialTheme.typography.bodyMedium,
    fullTextResId = R.string.signup_request_access,
    linkTextColor = MaterialTheme.colorScheme.primary,
    hyperLinks =
      mapOf(
        "sign_up_link" to
          {
            runCatching { uriHandler.openUri(signupLink) }
              .onFailure { Timber.e(it, "Failed to open sign-up link") }
          }
      ),
  )
}

@Preview
@ExcludeFromJacocoGeneratedReport
@Composable
fun PermissionDeniedDialogWithSignupLinkPreview() {
  AppTheme {
    PermissionDeniedDialog(signupLink = "www.google.com", onSignOut = {}, onCloseApp = {})
  }
}

@Preview
@ExcludeFromJacocoGeneratedReport
@Composable
fun PermissionDeniedDialogWithoutSignupLinkPreview() {
  AppTheme { PermissionDeniedDialog(signupLink = "", onSignOut = {}, onCloseApp = {}) }
}
