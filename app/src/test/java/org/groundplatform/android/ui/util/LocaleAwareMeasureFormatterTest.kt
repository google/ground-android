/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.util

import android.icu.util.MeasureUnit
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.Test
import org.groundplatform.android.BaseHiltTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocaleAwareMeasureFormatterTest : BaseHiltTest() {

  @Inject lateinit var formatter: LocaleAwareMeasureFormatter

  @Test
  fun `formatDistance returns metric formatting for small distances`() {
    val result = formatter.formatDistance(3.5, MeasureUnit.METER)

    assertThat(result).isEqualTo("3.5 m")
  }

  @Test
  fun `formatDistance returns metric formatting for large distances`() {
    val result = formatter.formatDistance(10.9, MeasureUnit.METER)

    assertThat(result).isEqualTo("10 m")
  }

  @Test
  fun `formatDistance returns imperial formatting for small distances`() {
    val result = formatter.formatDistance(3.0, MeasureUnit.FOOT)

    assertThat(result).isEqualTo("9.8 ft")
  }

  @Test
  fun `formatDistance returns imperial formatting for large distances`() {
    val result = formatter.formatDistance(10.0, MeasureUnit.FOOT)

    assertThat(result).isEqualTo("32 ft")
  }
}
