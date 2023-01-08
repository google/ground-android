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

import com.google.android.ground.model.submission.DateTaskData
import com.google.android.ground.model.submission.TimeTaskData
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.*
import org.junit.Test

class DateTimeTaskDataTest {
  @Test
  fun testTimeResponse_textDetails() {
    val instant = LocalDate.now().atTime(7, 30, 45).atZone(ZoneId.systemDefault()).toInstant()
    val detailsText = TimeTaskData(Date.from(instant)).getDetailsText()
    assertThat(detailsText).isEqualTo("07:30")
  }

  @Test
  fun testDateResponse_textDetails() {
    val instant =
      LocalDate.of(2021, Month.OCTOBER, 23)
        .atStartOfDay()
        .atZone(ZoneId.systemDefault())
        .toInstant()
    val detailsText = DateTaskData(Date.from(instant)).getDetailsText()
    assertThat(detailsText).isEqualTo("2021-10-23")
  }
}
