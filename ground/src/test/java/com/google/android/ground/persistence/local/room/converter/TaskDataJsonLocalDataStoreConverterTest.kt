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
package com.google.android.ground.persistence.local.room.converter

import com.google.android.ground.model.submission.DateTaskData
import com.google.android.ground.model.submission.TimeTaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.ResponseJsonConverter.dateToIsoString
import com.google.android.ground.persistence.local.room.converter.ResponseJsonConverter.isoStringToDate
import com.google.android.ground.persistence.local.room.converter.ResponseJsonConverter.toJsonObject
import com.google.android.ground.persistence.local.room.converter.ResponseJsonConverter.toResponse
import com.google.common.truth.Truth.assertThat
import java.util.*
import org.junit.Test

class TaskDataJsonLocalDataStoreConverterTest {

  @Test
  fun testDateToIsoString() {
    val dateToIsoString = dateToIsoString(DATE)
    assertThat(DATE_STRING).isEqualTo(dateToIsoString)
  }

  @Test
  fun testIsoStringToDate() {
    val stringToDate = isoStringToDate(DATE_STRING)
    assertThat(DATE).isEqualTo(stringToDate)
  }

  @Test
  fun testDateToIso_mismatchDate() {
    val currentDate = Date()
    val stringToDate = isoStringToDate(DATE_STRING)
    val checkMismatchDate = currentDate == stringToDate
    assertThat(checkMismatchDate).isEqualTo(false)
  }

  @Test
  fun testResponseToObject_dateResponse() {
    val response = toJsonObject(DateTaskData(DATE))
    assertThat(response).isInstanceOf(Any::class.java)
  }

  @Test
  fun testResponseToObject_timeResponse() {
    val response = toJsonObject(TimeTaskData(DATE))
    assertThat(response).isInstanceOf(Any::class.java)
  }

  @Test
  fun testObjectToResponse_dateResponse() {
    val dateObject = toJsonObject(DateTaskData(DATE))
    val response = toResponse(Task("1", 0, Task.Type.DATE, "date", true), dateObject)
    assertThat((response.get() as DateTaskData).date).isEqualTo(DATE)
  }

  @Test
  fun testObjectToResponse_timeResponse() {
    val timeObject = toJsonObject(TimeTaskData(DATE))
    val response = toResponse(Task("2", 1, Task.Type.TIME, "time", true), timeObject)
    assertThat((response.get() as TimeTaskData).time).isEqualTo(DATE)
  }

  companion object {
    // Date represented in YYYY-MM-DDTHH:mmZ Format from 1632501600000L milliseconds.
    private const val DATE_STRING = "2021-09-21T07:00+0000"

    // Date represented in milliseconds for date: 2021-09-24T16:40+0000.
    private val DATE = Date(1632207600000L)
  }
}
