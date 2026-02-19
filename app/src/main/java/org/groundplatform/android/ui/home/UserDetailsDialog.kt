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
package org.groundplatform.android.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.groundplatform.android.R
import org.groundplatform.android.model.User
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun UserDetailsDialog(user: User, signOutCallback: () -> Unit, dismissCallback: () -> Unit) {
  // In this AlertDialog instance, the button roles are reversed to match the provided design:
  // `dismissButton` is used for "Sign Out" and `confirmButton` is used for "Close".
  // This arrangement is due to the AlertDialog's fixed order of dismiss and confirm buttons.
  AlertDialog(
    onDismissRequest = { dismissCallback() },
    title = { Text(user.displayName) },
    text = { Text(user.email) },
    dismissButton = {
      TextButton(onClick = { signOutCallback() }) { Text(text = stringResource(R.string.sign_out)) }
    },
    confirmButton = {
      OutlinedButton(onClick = { dismissCallback() }) {
        Text(text = stringResource(R.string.close))
      }
    },
  )
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
private fun PreviewUserDetailsDialog() {
  AppTheme {
    UserDetailsDialog(
      user = User("id_1", "random@email.com", "Dummy name"),
      signOutCallback = {},
      dismissCallback = {},
    )
  }
}
