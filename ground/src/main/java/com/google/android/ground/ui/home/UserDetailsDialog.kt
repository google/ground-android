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

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.ground.R
import com.google.android.material.color.MaterialColors

@Composable
fun UserDetailsDialog(context: Context, homeScreenViewModel: HomeScreenViewModel) {
  val user = homeScreenViewModel.userDetails.value
  fun dismissDialog() {
    homeScreenViewModel.dismissDialogs()
  }
  AlertDialog(
    onDismissRequest = { dismissDialog() },
    title = { user?.displayName?.let { Text(it) } },
    text = { user?.email?.let { Text(it) } },
    dismissButton = {
      OutlinedButton(onClick = { dismissDialog() }) {
        Text(
          text = context.getString(R.string.cancel),
          color = Color(MaterialColors.getColor(context, R.attr.colorOnSurface, "")),
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { homeScreenViewModel.showSignOutConfirmationDialog() }) {
        Text(
          text = context.getString(R.string.sign_out),
          color = Color(MaterialColors.getColor(context, R.attr.colorError, "")),
        )
      }
    },
  )
}
