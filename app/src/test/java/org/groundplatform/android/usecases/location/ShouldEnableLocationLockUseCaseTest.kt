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
package org.groundplatform.android.usecases.location

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.ui.map.CameraPosition
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ShouldEnableLocationLockUseCaseTest : BaseHiltTest() {

  @BindValue @Mock lateinit var loiRepository: LocationOfInterestRepository
  @BindValue @Mock lateinit var mapStateRepository: MapStateRepository

  @Inject lateinit var shouldEnableLocationLockUseCase: ShouldEnableLocationLockUseCase

  @Test
  fun `returns false when survey has valid lois`() = runWithTestDispatcher {
    whenever(loiRepository.hasValidLois("survey")).thenReturn(true)

    assertThat(shouldEnableLocationLockUseCase("survey")).isFalse()
  }

  @Test
  fun `returns false when survey has valid lois and last saved camera position`() =
    runWithTestDispatcher {
      whenever(loiRepository.hasValidLois("survey")).thenReturn(true)
      whenever(mapStateRepository.getCameraPosition("survey"))
        .thenReturn(CameraPosition(Coordinates(0.0, 0.0)))

      assertThat(shouldEnableLocationLockUseCase("survey")).isFalse()
    }

  @Test
  fun `returns false when survey has no valid lois but has last saved camera position`() =
    runWithTestDispatcher {
      whenever(loiRepository.hasValidLois("survey")).thenReturn(false)
      whenever(mapStateRepository.getCameraPosition("survey"))
        .thenReturn(CameraPosition(Coordinates(0.0, 0.0)))

      assertThat(shouldEnableLocationLockUseCase("survey")).isFalse()
    }

  @Test
  fun `returns false when survey has no valid lois and no last saved camera position`() =
    runWithTestDispatcher {
      whenever(loiRepository.hasValidLois("survey")).thenReturn(false)
      whenever(mapStateRepository.getCameraPosition("survey")).thenReturn(null)

      assertThat(shouldEnableLocationLockUseCase("survey")).isTrue()
    }
}
