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
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CaptureLocationTaskViewModelTest : BaseHiltTest() {

  private val viewModel = CaptureLocationTaskViewModel()

  @Test
  fun testIsCaptureEnabled_whenLocationIsNull_returnsFalse() = runTest {
    assertThat(viewModel.isCaptureEnabled.first()).isFalse()
  }

  @Test
  fun testIsCaptureEnabled_whenAccuracyIsGood_returnsTrue() = runTest {
    setMockLocation(true, 10.0f)

    assertThat(viewModel.isCaptureEnabled.first()).isTrue()
  }

  @Test
  fun testIsCaptureEnabled_whenAccuracyIsPoor_returnsFalse() = runTest {
    setMockLocation(true, 20.0f)
    assertThat(viewModel.isCaptureEnabled.first()).isFalse()
  }

  @Test
  fun testIsCaptureEnabled_whenAccuracyIsBoundary_returnsTrue() = runTest {
    setMockLocation(true, 15.0f)
    assertThat(viewModel.isCaptureEnabled.first()).isTrue()
  }

  @Test
  fun testIsCaptureEnabled_whenAccuracyIsMissing_returnsFalse() = runTest {
    setMockLocation(false)
    assertThat(viewModel.isCaptureEnabled.first()).isFalse()
  }

  @Test(expected = IllegalStateException::class)
  fun testUpdateResponse_whenAccuracyIsPoor_throwsError() = runTest {
    setMockLocation(true, 20.0f)
    viewModel.updateResponse()
  }

  @Test
  fun testUpdateResponse_whenAccuracyIsGood_updatesResponse() = runTest {
    setMockLocation(true, 10.0f)
    viewModel.updateResponse()

    val data = viewModel.taskTaskData.value as CaptureLocationTaskData
    assertThat(data.accuracy).isEqualTo(10.0)
  }

  private fun setMockLocation(hasAccuracy: Boolean, accuracy: Float? = null) {
    val location = mock(Location::class.java)
    `when`(location.hasAccuracy()).thenReturn(hasAccuracy)
    accuracy?.let { `when`(location.accuracy).thenReturn(it) }
    viewModel.updateLocation(location)
  }
}
