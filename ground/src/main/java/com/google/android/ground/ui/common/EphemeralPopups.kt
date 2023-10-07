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

import android.app.Application
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.ground.R
import javax.inject.Inject
import javax.inject.Singleton

/** Displays short-lived messages such as toasts that are shown over other UI elements. */
@Singleton
class EphemeralPopups @Inject constructor(private val context: Application) {

  fun showError(@StringRes messageId: Int) = showLong(messageId)

  fun showError(message: String) = showLong(message)

  // TODO: Rename to unknownError?
  fun showError() = showLong(R.string.unexpected_error)

  private fun showLong(@StringRes messageId: Int) =
    Toast.makeText(context, messageId, Toast.LENGTH_LONG).show()

  private fun showLong(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
