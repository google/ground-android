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

import com.google.android.ground.model.submission.DateTimeTaskData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DateTimeTaskDataTest {

  @Test
  fun testTimeResponse_textDetails() {
    val calendarTime = DateTimeTaskData.fromMillis(1672549215471)
    assertThat(calendarTime).isEqualTo(DateTimeTaskData(timeInMillis = 1672549215471))
  }
}
