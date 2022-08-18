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
package com.google.android.ground.model.submission

import java8.util.Optional
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * A user-provided response to a date question task.
 */
@Serializable
class DateResponse(val date: @Contextual Date) : Response {
    // TODO(#752): Use device localization preferences.
    private val dateFormat: @Contextual DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun getSummaryText(): String = detailsText

    override fun getDetailsText(): String =
        synchronized(dateFormat) { dateFormat.format(date) }

    override fun isEmpty(): Boolean = date.time == 0L

    override fun equals(obj: Any?): Boolean = if (obj is DateResponse) {
        date === obj.date
    } else false

    override fun hashCode(): Int = date.toString().hashCode()

    override fun toString(): String = date.toString()

    companion object {
        @JvmStatic
        fun fromDate(date: Date): Optional<Response> {
            return if (date.time == 0L) Optional.empty() else Optional.of(DateResponse(date))
        }
    }
}