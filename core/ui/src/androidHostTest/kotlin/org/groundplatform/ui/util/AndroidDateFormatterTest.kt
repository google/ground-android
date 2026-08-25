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

import java.util.Locale
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidDateFormatterTest {

  private val formatter = AndroidDateFormatter(RuntimeEnvironment.getApplication())

  private val millis = 987654321L
  private val defaultLocale = Locale.getDefault()
  private val defaultZone = TimeZone.getDefault()

  @AfterTest
  fun tearDown() {
    Locale.setDefault(defaultLocale)
    TimeZone.setDefault(defaultZone)
  }

  @Test
  fun `formatDate correctly renders date with US locale and UTC timezone`() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    assertEquals("1/12/70", formatter.formatDate(millis))
  }

  @Test
  fun `formatTime correctly renders time with US locale and UTC timezone`() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    assertEquals("10:20 AM", formatter.formatTime(millis))
  }

  @Test
  fun `formatDateTime renders using the provided pattern in UTC timezone`() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    assertEquals("19700112 10:20", formatter.formatDateTime(millis, "yyyyMMdd HH:mm"))
  }
}
