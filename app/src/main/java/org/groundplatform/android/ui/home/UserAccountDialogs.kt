/*
 * Copyright 2026 Google LLC
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

import androidx.compose.runtime.Composable
import org.groundplatform.android.R
import org.groundplatform.android.model.User
import org.groundplatform.android.ui.components.ConfirmationDialog

@Composable
fun UserAccountDialogs(
  state: HomeScreenViewModel.AccountDialogState,
  user: User?,
  onSignOut: () -> Unit,
  onShowSignOutConfirmation: () -> Unit,
  onDismiss: () -> Unit,
) {
  when (state) {
    HomeScreenViewModel.AccountDialogState.USER_DETAILS ->
      user?.let {
        UserDetailsDialog(
          user = it,
          signOutCallback = onShowSignOutConfirmation,
          dismissCallback = onDismiss,
        )
      }
    HomeScreenViewModel.AccountDialogState.SIGN_OUT_CONFIRMATION ->
      ConfirmationDialog(
        title = R.string.sign_out_dialog_title,
        description = R.string.sign_out_dialog_body,
        confirmButtonText = R.string.sign_out,
        onConfirmClicked = onSignOut,
        onDismiss = onDismiss,
      )
    else -> {}
  }
}
