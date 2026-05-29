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
package org.groundplatform.ui.util

import android.content.Context
import android.text.format.DateFormat
import java.util.Date

/**
 * Android [DateFormatter] backed by [DateFormat], which respects system date/time settings (such as
 * the user's 24-hour preference) in addition to locale rules.
 */
class AndroidDateFormatter(private val context: Context) : DateFormatter {

  override fun formatDate(millis: Long): String =
    DateFormat.getDateFormat(context).format(Date(millis))

  override fun formatTime(millis: Long): String =
    DateFormat.getTimeFormat(context).format(Date(millis))
}
