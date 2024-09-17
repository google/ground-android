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
package com.google.android.ground.ui.datacollection.tasks.date

import android.content.Context
import android.text.format.DateFormat
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.*
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DateTaskViewModelTest : BaseHiltTest() {
  @Inject lateinit var dateTaskViewModel: DateTaskViewModel
  private lateinit var context: Context
  private lateinit var dateFormatter: java.text.DateFormat

  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
    dateFormatter = DateFormat.getDateFormat(context)
  }

  @Test
  fun testUpdateResponse() = runWithTestDispatcher {
    dateTaskViewModel.updateResponse(dateFormatter, CALENDAR.time)

    dateTaskViewModel.dateText.test {
      assertThat(expectMostRecentItem()).isEqualTo(dateFormatter.format(CALENDAR.time))
    }
  }

  companion object {
    private val CALENDAR = Calendar.getInstance()
  }
}
