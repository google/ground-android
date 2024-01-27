/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.ui.common

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.ground.databinding.ProgressDialogBinding

// TODO(#712): Replace with custom View.
object ProgressDialogs {

  fun Fragment.modalSpinner(@StringRes messageId: Int): AlertDialog {
    val binding: ProgressDialogBinding = ProgressDialogBinding.inflate(layoutInflater)
    binding.textProgressBar.text = getString(messageId)
    val dialog = AlertDialog.Builder(context).setView(binding.root).create()
    dialog.setCanceledOnTouchOutside(false)
    dialog.setCancelable(false)
    return dialog
  }

  fun modalSpinner(context: Context, @StringRes messageId: Int): ProgressDialog {
    val dialog = ProgressDialog(context)
    dialog.setMessage(context.resources.getString(messageId))
    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
    dialog.setCancelable(false)
    dialog.setCanceledOnTouchOutside(false)
    return dialog
  }
}
