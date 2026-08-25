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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970

/** iOS [DateFormatter] backed by [NSDateFormatter], which is locale- and settings-aware. */
@OptIn(ExperimentalForeignApi::class)
class IosDateFormatter : DateFormatter {

  override fun formatDate(millis: Long): String =
    NSDateFormatter()
      .apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
      }
      .stringFromDate(millis.toNSDate())

  override fun formatTime(millis: Long): String =
    NSDateFormatter()
      .apply {
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterShortStyle
      }
      .stringFromDate(millis.toNSDate())

  override fun formatDateTime(millis: Long, pattern: String): String =
    NSDateFormatter().apply { dateFormat = pattern }.stringFromDate(millis.toNSDate())

  private fun Long.toNSDate(): NSDate = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
}
