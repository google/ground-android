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
package org.groundplatform.android.ui.datacollection.tasks.location

import android.location.Location
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.BaseHiltTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CaptureLocationTaskViewModelTest : BaseHiltTest() {

  @Test
  fun testIsCaptureEnabled_whenLocationIsNull_returnsTrue() = runTest {
    val viewModel = CaptureLocationTaskViewModel()
    assertThat(viewModel.isCaptureEnabled.first()).isTrue()
  }

  @Test
  fun testIsCaptureEnabled_whenAccuracyIsGood_returnsTrue() = runTest {
    val viewModel = CaptureLocationTaskViewModel()
    val location = mock(Location::class.java)
    `when`(location.hasAccuracy()).thenReturn(true)
    `when`(location.accuracy).thenReturn(10.0f)
    viewModel.updateLocation(location)

    assertThat(viewModel.isCaptureEnabled.first()).isTrue()
  }

  @Test
  fun testIsCaptureEnabled_whenAccuracyIsPoor_returnsFalse() = runTest {
    val viewModel = CaptureLocationTaskViewModel()
    val location = mock(Location::class.java)
    `when`(location.hasAccuracy()).thenReturn(true)
    `when`(location.accuracy).thenReturn(20.0f)
    viewModel.updateLocation(location)

    assertThat(viewModel.isCaptureEnabled.first()).isFalse()
  }

  @Test
  fun testIsCaptureEnabled_whenAccuracyIsBoundary_returnsTrue() = runTest {
    val viewModel = CaptureLocationTaskViewModel()
    val location = mock(Location::class.java)
    `when`(location.hasAccuracy()).thenReturn(true)
    `when`(location.accuracy).thenReturn(15.0f)
    viewModel.updateLocation(location)

    assertThat(viewModel.isCaptureEnabled.first()).isTrue()
  }

  @Test
  fun testIsCaptureEnabled_whenAccuracyIsMissing_returnsFalse() = runTest {
    val viewModel = CaptureLocationTaskViewModel()
    val location = mock(Location::class.java)
    `when`(location.hasAccuracy()).thenReturn(false)
    viewModel.updateLocation(location)

    assertThat(viewModel.isCaptureEnabled.first()).isFalse()
  }

  @Test
  fun testUpdateResponse_whenAccuracyIsPoor_doesNotUpdateResponse() = runTest {
    val viewModel = CaptureLocationTaskViewModel()
    val location = mock(Location::class.java)
    `when`(location.hasAccuracy()).thenReturn(true)
    `when`(location.accuracy).thenReturn(20.0f)
    viewModel.updateLocation(location)

    viewModel.updateResponse()

    assertThat(viewModel.taskTaskData.value).isNull()
  }
}
