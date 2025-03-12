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
package org.groundplatform.android.ui.common

import android.app.Application
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject
import javax.inject.Singleton
import org.groundplatform.android.R

/** Displays short-lived messages such as toasts that are shown over other UI elements. */
@Suppress("UseDataClass")
@Singleton
class EphemeralPopups @Inject constructor(private val context: Application) {

  enum class PopupDuration {
    SHORT,
    LONG,
    INDEFINITE,
  }

  /** Defines functions to render a popup that displays an error message to the user. */
  inner class ErrorPopup {
    fun show(@StringRes messageId: Int, duration: PopupDuration = PopupDuration.LONG) =
      showToast(messageId, duration)

    fun show(message: String, duration: PopupDuration = PopupDuration.LONG) =
      showToast(message, duration)

    fun unknownError() = showToast(R.string.unexpected_error, duration = PopupDuration.LONG)

    private fun showToast(@StringRes messageId: Int, duration: PopupDuration) =
      showToast(context.getString(messageId), duration)

    private fun showToast(message: String, duration: PopupDuration) {
      val dur =
        if (duration == PopupDuration.SHORT) {
          Toast.LENGTH_SHORT
        } else {
          // INDEFINITE Length is not supported for toasts; we just use LONG instead for now.
          Toast.LENGTH_LONG
        }
      Toast.makeText(context, message, dur).show()
    }
  }

  /** Defines functions to render a popup that displays an informational message to the user. */
  inner class InfoPopup(view: View, @StringRes messageId: Int, duration: PopupDuration) {
    private var snackbar: Snackbar =
      Snackbar.make(view, context.resources.getString(messageId), durationToSnackDuration(duration))
        .setAction(context.getString(R.string.dismiss_info_popup)) { this.dismiss() }
    val isShown: Boolean
      get() = this.snackbar.isShown

    val view: View
      get() = this.snackbar.view

    fun show() = snackbar.show()

    fun setAnchor(view: View) = snackbar.setAnchorView(view)

    private fun dismiss() {
      this.snackbar.dismiss()
    }

    private fun durationToSnackDuration(duration: PopupDuration) =
      when (duration) {
        PopupDuration.SHORT -> Snackbar.LENGTH_SHORT
        PopupDuration.LONG -> Snackbar.LENGTH_LONG
        PopupDuration.INDEFINITE -> Snackbar.LENGTH_INDEFINITE
      }
  }
}
