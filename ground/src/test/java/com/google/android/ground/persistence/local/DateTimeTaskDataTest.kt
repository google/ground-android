/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.persistence.local

import android.content.Context
import android.text.format.DateFormat
import androidx.test.core.app.ApplicationProvider
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.submission.DateTaskData
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DateTimeTaskDataTest : BaseHiltTest() {

  private lateinit var context: Context

  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun testTimeResponse_textDetails() {
    val date = DateFormat.getTimeFormat(context).format(getCalendar().time)
    val detailsText = DateTaskData(date, CALENDAR.time.time).getDetailsText()
    assertThat(detailsText).isEqualTo("10:30 AM")
  }

  @Test
  fun testDateResponse_textDetails() {
    val date = DateFormat.getDateFormat(context).format(getCalendar().time)
    val detailsText = DateTaskData(date, CALENDAR.time.time).getDetailsText()
    assertThat(detailsText).isEqualTo("1/1/23")
  }

  companion object {
    private val CALENDAR = Calendar.getInstance()

    fun getCalendar(): Calendar {
      // January 1, 2023, 10:30:15 AM
      CALENDAR.set(Calendar.YEAR, 2023)
      CALENDAR.set(Calendar.MONTH, Calendar.JANUARY) // 0-based, so January is 0
      CALENDAR.set(Calendar.DAY_OF_MONTH, 1)
      CALENDAR.set(Calendar.HOUR_OF_DAY, 10) // 24-hour format
      CALENDAR.set(Calendar.MINUTE, 30)
      CALENDAR.set(Calendar.SECOND, 15)
      return CALENDAR
    }
  }
}
