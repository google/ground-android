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
package com.google.android.ground.ui.datacollection.tasks.time

import android.content.Context
import android.text.format.DateFormat
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.submission.DateTaskData.Companion.fromDate
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.*
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TimeTaskViewModelTest : BaseHiltTest() {
  @Inject lateinit var timeFieldViewModel: TimeTaskViewModel
  private lateinit var context: Context
  private lateinit var formattedDate: String

  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
    formattedDate = DateFormat.getDateFormat(context).format(CALENDAR.time)
  }

  @Test
  fun testUpdateResponse() = runWithTestDispatcher {
    timeFieldViewModel.updateResponse(formattedDate, CALENDAR.timeInMillis)

    timeFieldViewModel.taskTaskData.test {
      assertThat(expectMostRecentItem()).isEqualTo(fromDate(formattedDate, CALENDAR.timeInMillis))
    }
  }

  companion object {
    private val CALENDAR = Calendar.getInstance()
  }
}
