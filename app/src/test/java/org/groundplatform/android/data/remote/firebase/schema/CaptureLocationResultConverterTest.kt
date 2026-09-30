/*
 * Copyright 2026 Google LLC
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

package org.groundplatform.android.data.remote.firebase.schema

import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.data.remote.firebase.schema.CaptureLocationResultConverter.toCaptureLocationTaskData
import org.groundplatform.android.data.remote.firebase.schema.CaptureLocationResultConverter.toJSONObject
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.submission.CaptureLocationTaskData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureLocationResultConverterTest {

  @Test
  fun `toJSONObject writes accuracy, altitude and geometry`() {
    val json = CAPTURE_LOCATION.toJSONObject()

    assertThat(json.getDouble("accuracy")).isEqualTo(5.0)
    assertThat(json.getDouble("altitude")).isEqualTo(100.0)
    assertThat(json.has("geometry")).isTrue()
  }

  @Test
  fun `round trip preserves the captured location`() {
    val restored = CAPTURE_LOCATION.toJSONObject().toCaptureLocationTaskData()

    assertThat(restored).isEqualTo(CAPTURE_LOCATION)
  }

  private companion object {
    val CAPTURE_LOCATION =
      CaptureLocationTaskData(
        location = Point(Coordinates(10.0, 20.0)),
        altitude = 100.0,
        accuracy = 5.0,
      )
  }
}
