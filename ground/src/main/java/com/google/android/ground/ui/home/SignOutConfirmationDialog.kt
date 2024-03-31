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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import com.google.android.ground.R
import com.google.android.material.color.MaterialColors

@Composable
fun SignOutConfirmationDialog(
  context: Context,
  openDialog: MutableState<Boolean>,
  homeScreenViewModel: HomeScreenViewModel,
  signOutCallback: () -> Unit,
) {

  fun dismissDialog() {
    openDialog.value = false
  }

  when {
    openDialog.value ->
      AlertDialog(
        onDismissRequest = { dismissDialog() },
        title = { Text(text = context.getString(R.string.sign_out_dialog_title)) },
        text = { Text(text = context.getString(R.string.sign_out_dialog_body)) },
        dismissButton = {
          TextButton(onClick = { dismissDialog() }) {
            Text(
              text = context.getString(R.string.cancel),
              color = Color(MaterialColors.getColor(context, R.attr.colorOnSurface, "")),
            )
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              signOutCallback()
              dismissDialog()
            }
          ) {
            Text(
              text = context.getString(R.string.sign_out),
              color = Color(MaterialColors.getColor(context, R.attr.colorError, "")),
            )
          }
        },
      )
  }

//  ReusableAlertDialog(
//    dismissDialog = { /*TODO*/},
//    signOutCallback = { signOutCallback},
//    title = { homeScreenViewModel.getUserData()?.displayName },
//    text = { homeScreenViewModel.getUserData()?.email },
//    context = context,
//  )
}

@Composable
private fun ReusableAlertDialog(
  dismissDialog: () -> Unit,
  signOutCallback: () -> Unit,
  title: @Composable () -> Unit,
  text: @Composable () -> Unit,
  context: Context,
) {
  AlertDialog(
    onDismissRequest = { dismissDialog },
    title = { title },
    text = { text },
    confirmButton = {
      TextButton(
        onClick = {
          signOutCallback()
          dismissDialog()
        }
      ) {
        Text(
          text = context.getString(R.string.sign_out),
          color = Color(MaterialColors.getColor(context, R.attr.colorError, "")),
        )
      }
    },
    dismissButton = {
      OutlinedButton(onClick = { dismissDialog() }) {
        Text(
          text = context.getString(R.string.cancel),
          color = Color(MaterialColors.getColor(context, R.attr.colorOnSurface, "")),
        )
      }
    },
  )
}
